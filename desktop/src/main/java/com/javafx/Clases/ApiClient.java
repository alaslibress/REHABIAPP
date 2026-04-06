package com.javafx.Clases;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.javafx.dto.ErrorResponse;
import com.javafx.dto.LoginRequest;
import com.javafx.dto.LoginResponse;
import com.javafx.dto.RefreshRequest;
import com.javafx.excepcion.AutenticacionException;
import com.javafx.excepcion.ConexionException;
import com.javafx.excepcion.DuplicadoException;
import com.javafx.excepcion.PermisoException;
import com.javafx.excepcion.ValidacionException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;

/**
 * Cliente HTTP central que reemplaza ConexionBD.
 * Gestiona los tokens JWT y todas las llamadas a la API REST.
 * Singleton thread-safe.
 */
public class ApiClient {

    private static volatile ApiClient instancia;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final Duration timeout;

    // Tokens JWT en memoria
    private volatile String accessToken;
    private volatile String refreshToken;

    private ApiClient() {
        // Cargar configuracion desde api.properties
        Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/config/api.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            // Usar valores por defecto si no se puede cargar el archivo
        }
        this.baseUrl = props.getProperty("api.base-url", "http://localhost:8080");
        long timeoutMs = Long.parseLong(props.getProperty("api.timeout-ms", "30000"));
        this.timeout = Duration.ofMillis(timeoutMs);

        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(this.timeout)
            .build();

        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Constructor para pruebas unitarias. Permite inyectar un HttpClient mock.
     * No usar en produccion — el codigo de produccion usa getInstancia().
     */
    ApiClient(HttpClient httpClientTest, String baseUrlTest, long timeoutMs) {
        this.baseUrl = baseUrlTest;
        this.timeout = Duration.ofMillis(timeoutMs);
        this.httpClient = httpClientTest;
        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Obtiene la instancia unica del ApiClient.
     */
    public static ApiClient getInstancia() {
        if (instancia == null) {
            synchronized (ApiClient.class) {
                if (instancia == null) {
                    instancia = new ApiClient();
                }
            }
        }
        return instancia;
    }

    // ==================== CONEXION Y SESION ====================

    /**
     * Verifica la disponibilidad de la API via GET /actuator/health.
     * @return true si la API responde correctamente
     */
    public boolean probarConexion() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/actuator/health"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Realiza login via POST /api/auth/login y almacena los tokens JWT.
     * @param dni DNI del sanitario
     * @param contrasena Contrasena en texto plano
     * @return LoginResponse con tokens y rol
     */
    public LoginResponse login(String dni, String contrasena) {
        LoginRequest body = new LoginRequest(dni, contrasena);
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/auth/login"))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                LoginResponse loginResponse = objectMapper.readValue(response.body(), LoginResponse.class);
                this.accessToken = loginResponse.accessToken();
                this.refreshToken = loginResponse.refreshToken();
                return loginResponse;
            }

            manejarErrorHttp(response.statusCode(), response.body());
            return null; // No alcanzable, manejarErrorHttp siempre lanza excepcion
        } catch (ConexionException | AutenticacionException | PermisoException |
                 ValidacionException | DuplicadoException e) {
            throw e;
        } catch (Exception e) {
            throw new ConexionException("Error al conectar con la API: " + e.getMessage(), e);
        }
    }

    /**
     * Cierra la sesion limpiando los tokens en memoria.
     */
    public void logout() {
        this.accessToken = null;
        this.refreshToken = null;
    }

    /**
     * Indica si hay una sesion activa (accessToken presente).
     */
    public boolean haySesionActiva() {
        return accessToken != null && !accessToken.isEmpty();
    }

    /**
     * Devuelve el accessToken actual.
     */
    public String getAccessToken() {
        return accessToken;
    }

    // ==================== HTTP GENERICOS ====================

    /**
     * GET con deserializacion a clase simple.
     */
    public <T> T get(String path, Class<T> responseType) {
        return ejecutarConReintento(() -> ejecutarGet(path, responseType));
    }

    /**
     * GET con deserializacion a tipo generico (List, PageResponse, etc.).
     */
    public <T> T get(String path, TypeReference<T> responseType) {
        return ejecutarConReintento(() -> ejecutarGetGenerico(path, responseType));
    }

    /**
     * POST con body JSON y deserializacion de respuesta.
     */
    public <T> T post(String path, Object body, Class<T> responseType) {
        return ejecutarConReintento(() -> ejecutarPost(path, body, responseType));
    }

    /**
     * PUT con body JSON y deserializacion de respuesta.
     */
    public <T> T put(String path, Object body, Class<T> responseType) {
        return ejecutarConReintento(() -> ejecutarPut(path, body, responseType));
    }

    /**
     * DELETE sin parametros adicionales.
     */
    public void delete(String path) {
        ejecutarConReintento(() -> {
            ejecutarDelete(path);
            return null;
        });
    }

    /**
     * DELETE con query params adicionales.
     */
    public void delete(String path, Map<String, String> queryParams) {
        StringBuilder sb = new StringBuilder(path);
        if (queryParams != null && !queryParams.isEmpty()) {
            sb.append("?");
            queryParams.forEach((k, v) -> sb.append(k).append("=").append(v).append("&"));
            sb.deleteCharAt(sb.length() - 1);
        }
        delete(sb.toString());
    }

    // ==================== IMPLEMENTACIONES INTERNAS ====================

    private <T> T ejecutarGet(String path, Class<T> responseType) {
        try {
            HttpRequest request = buildGetRequest(path);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), responseType);
            }
            manejarErrorHttp(response.statusCode(), response.body());
            return null;
        } catch (ConexionException | AutenticacionException | PermisoException |
                 ValidacionException | DuplicadoException e) {
            throw e;
        } catch (Exception e) {
            throw new ConexionException("Error al conectar con la API: " + e.getMessage(), e);
        }
    }

    private <T> T ejecutarGetGenerico(String path, TypeReference<T> responseType) {
        try {
            HttpRequest request = buildGetRequest(path);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), responseType);
            }
            manejarErrorHttp(response.statusCode(), response.body());
            return null;
        } catch (ConexionException | AutenticacionException | PermisoException |
                 ValidacionException | DuplicadoException e) {
            throw e;
        } catch (Exception e) {
            throw new ConexionException("Error al conectar con la API: " + e.getMessage(), e);
        }
    }

    private <T> T ejecutarPost(String path, Object body, Class<T> responseType) {
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                if (responseType == Void.class || response.body() == null || response.body().isEmpty()) {
                    return null;
                }
                return objectMapper.readValue(response.body(), responseType);
            }
            manejarErrorHttp(response.statusCode(), response.body());
            return null;
        } catch (ConexionException | AutenticacionException | PermisoException |
                 ValidacionException | DuplicadoException e) {
            throw e;
        } catch (Exception e) {
            throw new ConexionException("Error al conectar con la API: " + e.getMessage(), e);
        }
    }

    private <T> T ejecutarPut(String path, Object body, Class<T> responseType) {
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                if (responseType == Void.class || response.body() == null || response.body().isEmpty()) {
                    return null;
                }
                return objectMapper.readValue(response.body(), responseType);
            }
            manejarErrorHttp(response.statusCode(), response.body());
            return null;
        } catch (ConexionException | AutenticacionException | PermisoException |
                 ValidacionException | DuplicadoException e) {
            throw e;
        } catch (Exception e) {
            throw new ConexionException("Error al conectar con la API: " + e.getMessage(), e);
        }
    }

    private void ejecutarDelete(String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(timeout)
                .header("Authorization", "Bearer " + accessToken)
                .DELETE()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 || response.statusCode() == 204) {
                return;
            }
            manejarErrorHttp(response.statusCode(), response.body());
        } catch (ConexionException | AutenticacionException | PermisoException |
                 ValidacionException | DuplicadoException e) {
            throw e;
        } catch (Exception e) {
            throw new ConexionException("Error al conectar con la API: " + e.getMessage(), e);
        }
    }

    // ==================== TOKEN REFRESH ====================

    /**
     * Intenta renovar el accessToken usando el refreshToken.
     * @return true si el refresh fue exitoso
     */
    private boolean renovarToken() {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return false;
        }
        try {
            RefreshRequest body = new RefreshRequest(refreshToken);
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/auth/refresh"))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                LoginResponse loginResponse = objectMapper.readValue(response.body(), LoginResponse.class);
                this.accessToken = loginResponse.accessToken();
                this.refreshToken = loginResponse.refreshToken();
                return true;
            }
        } catch (Exception e) {
            // Si falla el refresh, la sesion expiro
        }
        return false;
    }

    /**
     * Ejecuta la operacion y, si recibe 401, intenta renovar el token y reintenta una vez.
     */
    private <T> T ejecutarConReintento(OperacionHttp<T> operacion) {
        try {
            return operacion.ejecutar();
        } catch (AutenticacionException e) {
            // Intentar renovar el token una vez
            if (renovarToken()) {
                return operacion.ejecutar();
            }
            // Si falla el refresh, limpiar sesion y relanzar
            logout();
            throw e;
        }
    }

    // ==================== UTILIDADES ====================

    private HttpRequest buildGetRequest(String path) {
        return HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .timeout(timeout)
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();
    }

    /**
     * Traduce codigos de error HTTP a excepciones del dominio.
     */
    private void manejarErrorHttp(int statusCode, String responseBody) {
        String mensaje = extraerMensajeError(responseBody);
        switch (statusCode) {
            case 400 -> throw new ValidacionException(mensaje, "peticion");
            case 401 -> throw new AutenticacionException(mensaje);
            case 403 -> throw new PermisoException(mensaje);
            case 404 -> throw new ValidacionException(mensaje, "entidad");
            case 409 -> throw new DuplicadoException(mensaje, "registro");
            default -> throw new ConexionException("Error de servidor (" + statusCode + "): " + mensaje);
        }
    }

    /**
     * Extrae el campo message del JSON de error de la API, o devuelve el body completo.
     */
    private String extraerMensajeError(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return "Error desconocido";
        }
        try {
            ErrorResponse error = objectMapper.readValue(responseBody, ErrorResponse.class);
            return error.message() != null ? error.message() : responseBody;
        } catch (Exception e) {
            return responseBody;
        }
    }

    // ==================== INTERFAZ FUNCIONAL ====================

    @FunctionalInterface
    private interface OperacionHttp<T> {
        T ejecutar();
    }
}
