# Credenciales de acceso — Usuario de prueba

Validas unicamente con el BFF arrancado en modo mock (`MOCK_API=true`).
La API de Java no necesita estar activa.

---

## Usuario admin

| Campo              | Valor                  |
|--------------------|------------------------|
| Nombre completo    | Admin RehabiAPP        |
| DNI                | 12345678Z              |
| Email              | admin@rehabiapp.com    |
| Contrasena         | admin                  |
| Fecha nacimiento   | 01/01/1990             |
| Telefono           | 600 000 000            |
| Sexo               | Masculino              |
| Num. S. Social     | 280000000001           |

El DNI 12345678Z es valido segun el algoritmo oficial espanol (12345678 mod 23 = 14 → letra Z).

---

## Datos clinicos de prueba

### Discapacidades

| Codigo | Nombre              | Nivel       |
|--------|---------------------|-------------|
| M16    | Coxartrosis         | Intermedio  |
| M54    | Lumbalgia cronica   | Inicial     |

### Tratamientos asignados

| Codigo | Nombre                                  | Visible |
|--------|-----------------------------------------|---------|
| TRT001 | Ejercicios de movilidad de cadera       | Si      |
| TRT002 | Electroterapia de baja frecuencia       | Si      |
| TRT003 | Ejercicios de fortalecimiento lumbar    | Si      |
| TRT004 | Hidroterapia terapeutica                | No      |

### Proximas citas

| Fecha       | Hora  | Sanitario  |
|-------------|-------|------------|
| 10/04/2026  | 10:00 | 87654321B  |
| 17/04/2026  | 11:30 | 87654321B  |
| 24/04/2026  | 09:00 | 87654321B  |

---

## Como iniciar sesion

El login acepta cualquiera de estos identificadores junto con la contrasena `admin`:

- Nombre de usuario: `admin`
- DNI: `12345678Z`
- Email: `admin@rehabiapp.com`

---

## Paso a paso: levantar el entorno de desarrollo

### Requisitos previos

- Node.js 20 o superior instalado
- npm instalado
- Expo Go instalado en el movil (o emulador Android/iOS)

---

### Paso 1 — Instalar dependencias del backend (primera vez)

```bash
cd mobile/backend
npm install
```

### Paso 2 — Arrancar el backend BFF en modo mock

Abrir una terminal y ejecutar:

```bash
cd mobile/backend
npm run dev
```

El servidor arranca en `http://localhost:3000`.
El modo mock esta activado por defecto en desarrollo (`MOCK_API=true` en `.env` o variable de entorno).

Para confirmarlo, debe aparecer en la terminal:

```
{"level":30,...,"msg":"BFF mobile-backend iniciado","port":3000,"mockApi":true}
```

Verificar que el backend responde:

```bash
curl http://localhost:3000/health
# Respuesta esperada: {"status":"UP"}
```

### Paso 3 — Instalar dependencias del frontend (primera vez)

Abrir otra terminal:

```bash
cd mobile/frontend
npm install
```

### Paso 4 — Arrancar el frontend con Expo

```bash
cd mobile/frontend
npm start
```

Expo mostrara un codigo QR en la terminal. Opciones para abrir la app:

| Opcion | Comando | Requisito |
|--------|---------|-----------|
| Movil fisico | Escanear QR con Expo Go | Mismo WiFi que el PC |
| Emulador Android | `npm run android` | Android Studio instalado |
| Emulador iOS | `npm run ios` | macOS + Xcode instalado |
| Navegador web | `npm run web` | Solo para pruebas de UI |

### Paso 5 — Iniciar sesion en la app

Una vez la app este abierta, usar cualquiera de estas credenciales:

| Campo       | Valor                 |
|-------------|-----------------------|
| Identificador | `admin`             |
| Contrasena  | `admin`               |

Tambien funciona con `admin@rehabiapp.com` o `12345678Z` como identificador.

---

### Verificacion rapida via curl (opcional)

Sin abrir la app, se puede probar el login directamente contra el BFF:

```bash
curl -X POST http://localhost:3000/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"mutation { login(identifier: \"admin\", password: \"admin\") { accessToken refreshToken expiresAt } }"}'
```

Respuesta esperada:

```json
{
  "data": {
    "login": {
      "accessToken": "eyJ...",
      "refreshToken": "eyJ...",
      "expiresAt": 1234567890
    }
  }
}
```

### Ejecutar los tests del backend

```bash
cd mobile/backend
npm test
# Resultado esperado: 16 tests, 0 fallos
```

---

> Este archivo es solo para desarrollo local. No contiene credenciales de produccion.
> En produccion los pacientes se autentican contra la API de Java con su DNI real y contrasena cifrada.
