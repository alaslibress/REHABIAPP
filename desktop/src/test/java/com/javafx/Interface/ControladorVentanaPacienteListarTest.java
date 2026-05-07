package com.javafx.Interface;

import com.javafx.Clases.Paciente;
import com.javafx.Clases.VentanaUtil;
import com.javafx.DAO.PacienteDAO;
import com.javafx.excepcion.ConexionException;
import com.javafx.excepcion.PermisoException;
import com.javafx.service.CatalogoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests de la logica defensiva del controlador de ficha de paciente.
 * Verifica que initialize() y cargarDatosPaciente() no propagen excepciones
 * del dominio al FXMLLoader aunque la API devuelva errores.
 *
 * Sin TestFX: instanciacion directa + reflexion para inyectar mocks.
 * Los campos @FXML se dejan en null (no se prueban las partes de UI).
 */
@ExtendWith(MockitoExtension.class)
class ControladorVentanaPacienteListarTest {

    @Mock
    private CatalogoService catalogoServiceMock;

    @Mock
    private PacienteDAO pacienteDAOMock;

    private controladorVentanaPacienteListar controlador;

    @BeforeEach
    void setUp() throws Exception {
        // Instanciacion directa: no pasa por FXMLLoader, campos @FXML quedan null
        controlador = new controladorVentanaPacienteListar();

        // Inyectar mapa de niveles (inicializado normalmente en initialize())
        setField(controlador, "mapaNiveles", new HashMap<>());
        setField(controlador, "mapaTratamientos", new HashMap<>());

        // Inyectar mocks en los campos de servicio
        setField(controlador, "catalogoService", catalogoServiceMock);
        setField(controlador, "pacienteDAO", pacienteDAOMock);
    }

    // ==================== K.1.1 — cargarMapaNiveles ====================

    /**
     * PermisoException de la API NO debe propagarse fuera de cargarMapaNiveles().
     * El mapa de niveles queda vacio pero la aplicacion no se cuelga.
     */
    @Test
    void cargarMapaNiveles_conPermisoException_noPropaga() throws Exception {
        when(catalogoServiceMock.listarNiveles())
                .thenThrow(new PermisoException("Acceso denegado a /api/catalogo/niveles-progresion"));

        // No debe lanzar ninguna excepcion
        assertDoesNotThrow(() -> invocarMetodoPrivado("cargarMapaNiveles"));

        // El mapa queda inicializado pero vacio
        HashMap<?, ?> mapaNiveles = (HashMap<?, ?>) getField(controlador, "mapaNiveles");
        assertNotNull(mapaNiveles, "mapaNiveles no debe ser null tras el error");
        assertTrue(mapaNiveles.isEmpty(), "mapaNiveles debe quedar vacio si el API fallo");
    }

    /**
     * ConexionException de la API tampoco debe propagarse.
     */
    @Test
    void cargarMapaNiveles_conConexionException_noPropaga() throws Exception {
        when(catalogoServiceMock.listarNiveles())
                .thenThrow(new ConexionException("Sin conexion"));

        assertDoesNotThrow(() -> invocarMetodoPrivado("cargarMapaNiveles"));

        HashMap<?, ?> mapaNiveles = (HashMap<?, ?>) getField(controlador, "mapaNiveles");
        assertNotNull(mapaNiveles);
        assertTrue(mapaNiveles.isEmpty());
    }

    /**
     * Si la API responde OK, el mapa se popula correctamente.
     */
    @Test
    void cargarMapaNiveles_conRespuestaOk_populaMapa() throws Exception {
        com.javafx.Clases.NivelProgresion nivel = mock(com.javafx.Clases.NivelProgresion.class);
        when(nivel.getNombreCorto()).thenReturn("FA");
        when(catalogoServiceMock.listarNiveles()).thenReturn(java.util.List.of(nivel));

        assertDoesNotThrow(() -> invocarMetodoPrivado("cargarMapaNiveles"));

        HashMap<?, ?> mapaNiveles = (HashMap<?, ?>) getField(controlador, "mapaNiveles");
        assertFalse(mapaNiveles.isEmpty(), "El mapa debe contener el nivel cargado");
    }

    // ==================== K.1.2 — cargarDatosPaciente ====================

    /**
     * PermisoException en obtenerPorDNI deja pacienteActual null y muestra modal.
     * La excepcion NO debe propagarse al caller.
     */
    @Test
    void cargarDatosPaciente_conPermisoException_dejaPacienteNull() throws Exception {
        when(catalogoServiceMock.listarTratamientos())
                .thenReturn(java.util.List.of());
        when(pacienteDAOMock.obtenerPorDNI("12345678Z"))
                .thenThrow(new PermisoException("Acceso denegado a /api/pacientes/12345678Z"));

        try (MockedStatic<VentanaUtil> ventanaUtilMock = mockStatic(VentanaUtil.class)) {
            assertDoesNotThrow(() -> controlador.cargarDatosPaciente("12345678Z"));

            // pacienteActual debe quedar null
            Object pacienteActual = getField(controlador, "pacienteActual");
            assertNull(pacienteActual, "pacienteActual debe ser null cuando el API devuelve 403");

            // El modal de error debe haberse mostrado
            ventanaUtilMock.verify(() ->
                VentanaUtil.mostrarVentanaInformativa(contains("permisos"), eq(VentanaUtil.TipoMensaje.ERROR)),
                times(1)
            );
        }
    }

    /**
     * ConexionException en obtenerPorDNI deja pacienteActual null con modal de conexion.
     */
    @Test
    void cargarDatosPaciente_conConexionException_dejaPacienteNull() throws Exception {
        when(catalogoServiceMock.listarTratamientos())
                .thenReturn(java.util.List.of());
        when(pacienteDAOMock.obtenerPorDNI("12345678Z"))
                .thenThrow(new ConexionException("Sin conexion"));

        try (MockedStatic<VentanaUtil> ventanaUtilMock = mockStatic(VentanaUtil.class)) {
            assertDoesNotThrow(() -> controlador.cargarDatosPaciente("12345678Z"));

            Object pacienteActual = getField(controlador, "pacienteActual");
            assertNull(pacienteActual);

            ventanaUtilMock.verify(() ->
                VentanaUtil.mostrarVentanaInformativa(contains("conexion"), eq(VentanaUtil.TipoMensaje.ERROR)),
                times(1)
            );
        }
    }

    /**
     * PermisoException en listarTratamientos (catalogo opcional) NO muestra modal
     * y el flujo continua normalmente hacia obtenerPorDNI.
     */
    @Test
    void cargarDatosPaciente_conPermisoEnCatalogo_continuaConObtenerPorDNI() throws Exception {
        when(catalogoServiceMock.listarTratamientos())
                .thenThrow(new PermisoException("Acceso denegado a catalogo"));
        // obtenerPorDNI retorna null (paciente no encontrado)
        when(pacienteDAOMock.obtenerPorDNI("12345678Z")).thenReturn(null);

        try (MockedStatic<VentanaUtil> ventanaUtilMock = mockStatic(VentanaUtil.class)) {
            assertDoesNotThrow(() -> controlador.cargarDatosPaciente("12345678Z"));

            // obtenerPorDNI SI se llama aunque el catalogo haya fallado
            verify(pacienteDAOMock, times(1)).obtenerPorDNI("12345678Z");

            // Se muestra "no encontrado" porque pacienteActual es null y no hubo excepcion en obtener
            ventanaUtilMock.verify(() ->
                VentanaUtil.mostrarVentanaInformativa(contains("No se encontro"), eq(VentanaUtil.TipoMensaje.ERROR)),
                times(1)
            );
        }
    }

    // ==================== Utilidades de reflexion ====================

    private void invocarMetodoPrivado(String nombre) throws Exception {
        Method m = controladorVentanaPacienteListar.class.getDeclaredMethod(nombre);
        m.setAccessible(true);
        m.invoke(controlador);
    }

    private void setField(Object target, String nombre, Object valor) throws Exception {
        // Buscar el campo en la clase o sus superclases
        Class<?> cls = target.getClass();
        while (cls != null) {
            try {
                Field f = cls.getDeclaredField(nombre);
                f.setAccessible(true);
                f.set(target, valor);
                return;
            } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Campo '" + nombre + "' no encontrado en " + target.getClass());
    }

    private Object getField(Object target, String nombre) throws Exception {
        Class<?> cls = target.getClass();
        while (cls != null) {
            try {
                Field f = cls.getDeclaredField(nombre);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Campo '" + nombre + "' no encontrado en " + target.getClass());
    }
}
