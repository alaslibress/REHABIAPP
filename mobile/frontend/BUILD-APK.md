# Build APK — RehabiAPP Mobile

Dos rutas: **local** (sin cuenta Expo, requiere Android SDK) y **EAS Cloud** (Expo construye en cloud, sin SDK local).

## Pre-requisitos comunes

| Item | Como instalar |
|------|---------------|
| Node 20+ | `nvm install 20 && nvm use 20` |
| `npm install` ejecutado | desde `mobile/frontend/` |
| BFF accesible publicamente | El backend EC2 ya expone `https://rehabiapp-api.duckdns.org/graphql` via Caddy |

La URL de produccion ya esta hardcodeada en `eas.json` + scripts npm como `EXPO_PUBLIC_API_URL=https://rehabiapp-api.duckdns.org`. Si tu BFF cambia de host, editar ahi.

---

## Opción A — Build local (recomendado para TFG sin cuenta Expo)

### Pre-requisitos extra
- **JDK 17 o 21** (Gradle 8 no soporta Java 24 todavia — si tienes 24 instala 21 paralelo):
  ```bash
  sudo dnf install java-21-openjdk java-21-openjdk-devel    # Fedora
  sudo update-alternatives --config java                     # elegir 21
  export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
  ```
- **Android SDK** (Build Tools, Platform 34+, NDK opcional). Si tienes Android Studio ya esta. Sino:
  ```bash
  # Variables (anadir a ~/.bashrc o ~/.zshrc)
  export ANDROID_HOME=$HOME/Android/Sdk
  export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin
  ```

### Build

```bash
cd mobile/frontend
npm install
npm run build:apk                # APK de release (firmada con debug key, valida para sideload)
```

El APK queda en:
```
mobile/frontend/android/app/build/outputs/apk/release/app-release.apk
```

Para una **APK debug** (instalable directamente sin firmar):
```bash
npm run build:apk:debug
# → android/app/build/outputs/apk/debug/app-debug.apk
```

### Instalar en dispositivo Android
```bash
# Con el movil conectado por USB + depuracion activada:
adb install -r android/app/build/outputs/apk/release/app-release.apk

# O copiar el .apk al movil + tap para instalar (permite "fuentes desconocidas").
```

### Firmar APK con keystore propio (opcional, requerido para Play Store)
```bash
keytool -genkeypair -v -storetype PKCS12 -keystore rehabiapp.keystore \
        -alias rehabiapp -keyalg RSA -keysize 2048 -validity 10000

# Anadir a android/gradle.properties (NO commitear):
# MYAPP_UPLOAD_STORE_FILE=rehabiapp.keystore
# MYAPP_UPLOAD_KEY_ALIAS=rehabiapp
# MYAPP_UPLOAD_STORE_PASSWORD=***
# MYAPP_UPLOAD_KEY_PASSWORD=***

# Editar android/app/build.gradle release signingConfig para usar estas vars.
```

---

## Opción B — EAS Build (cloud)

### Pre-requisitos extra
- Cuenta gratuita en https://expo.dev
- `eas-cli` global:
  ```bash
  npm install -g eas-cli
  eas login
  eas init --id <projectId-de-eas.dev>
  ```
- Editar `app.json` reemplazando el `extra.eas.projectId` placeholder por el id real que te dio `eas init`.

### Build

```bash
cd mobile/frontend
npm run build:eas:preview         # APK firmada para instalar (preview profile)
# o:
npm run build:eas:production      # AAB para Google Play (production profile)
```

EAS sube el código, construye en cloud y devuelve una URL de descarga del `.apk` o `.aab`.

### Ventajas EAS vs local
- Sin necesidad de SDK Android / Java en tu maquina.
- Firma con keystore gestionado por Expo (no se pierde).
- Build profiles distintos (`development`, `preview`, `production`) por entorno.
- Auto-increment de `versionCode`.

---

## Verificacion post-instalacion

1. Abrir la app en el movil.
2. Login con `11111111H` / `Juan1234!`.
3. Verificar:
   - Tab Perfil → avatar (si Juan tiene foto subida desde desktop).
   - Tab Citas → cita Juan 2026-05-19 12:30 con `Doctora Prueba Rehabilitacion`.
   - Tab Juegos → tap PIANO → abre Chrome/navegador con la WebGL.
   - Tab Progreso → muneco con mano derecha azul.

## Variables de entorno en runtime

Para apuntar a otro entorno (dev, staging) editar `eas.json` o pasar inline:
```bash
EXPO_PUBLIC_API_URL=http://192.168.1.100:3000 npm run build:apk:debug
```
(Useful para probar contra un BFF local sin tener que publicar.)

## Troubleshooting

| Síntoma | Causa | Solución |
|---------|-------|----------|
| `./gradlew assembleRelease` falla con `Unsupported class file major version` | JDK 24 (Gradle 8 no soporta) | Instalar JDK 21, `update-alternatives --config java` |
| `SDK location not found` | Falta `ANDROID_HOME` | `export ANDROID_HOME=$HOME/Android/Sdk` |
| `Manifest merger failed` tras prebuild | Algun plugin Expo desactualizado | `npx expo install --check` y aplicar fixes |
| App se cierra al abrir | `EXPO_PUBLIC_API_URL` apunta a host inaccesible | Verificar `curl https://rehabiapp-api.duckdns.org/actuator/health` |
| Notificaciones no llegan | Sin projectId EAS real | Aceptable — son locales; push remote requiere EAS real |
