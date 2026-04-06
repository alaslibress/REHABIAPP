# Plan: Corregir errores de ejecucion JavaFX desde IntelliJ IDEA

> **Fecha:** 2026-04-06
> **Rama:** desktop-final
> **Problema:** IntelliJ genera una tarea Gradle propia (`:com.javafx.Clases.Main.main()`) que no recibe los argumentos `--module-path` ni `--add-modules` del plugin javafx, causando "JavaFX runtime components are missing".

---

## FASE 1: Propagar argumentos JavaFX a TODAS las tareas JavaExec

### Descripcion del problema

El plugin `org.openjfx.javafxplugin` version 0.1.0 solo configura `--module-path` y `--add-modules` en la tarea estandar `run`. Cuando IntelliJ ejecuta la clase `Main` directamente, crea una tarea JavaExec separada que no hereda esa configuracion. El bloque `tasks.withType(JavaExec).configureEach` actual (lineas 124-134 de `build.gradle`) solo tiene `--add-opens`, pero le faltan los argumentos de modulos JavaFX.

### Solucion propuesta

Modificar el bloque `tasks.withType(JavaExec).configureEach` en `build.gradle` para inyectar `--module-path` y `--add-modules` dinamicamente, extrayendo la ruta de los JARs de JavaFX desde el classpath de runtime.

Reemplazar el bloque actual (lineas 124-134):

```groovy
tasks.withType(JavaExec).configureEach {
    jvmArgs += [
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED",
            "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
            "--add-opens=java.base/java.text=ALL-UNNAMED",
            "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
            "--add-opens=java.desktop/java.awt.geom=ALL-UNNAMED",
            "--add-opens=java.base/java.io=ALL-UNNAMED"
    ]
}
```

Por este bloque:

```groovy
tasks.withType(JavaExec).configureEach {
    // Apertura de modulos internos para JasperReports y otras libs en Java 24
    jvmArgs += [
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED",
            "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
            "--add-opens=java.base/java.text=ALL-UNNAMED",
            "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
            "--add-opens=java.desktop/java.awt.geom=ALL-UNNAMED",
            "--add-opens=java.base/java.io=ALL-UNNAMED"
    ]

    // Propagacion de --module-path y --add-modules a TODAS las tareas JavaExec,
    // incluidas las generadas automaticamente por IntelliJ IDEA.
    // Sin esto, IntelliJ falla con "JavaFX runtime components are missing".
    doFirst {
        def fxJars = configurations.runtimeClasspath.files
                .findAll { it.name.startsWith('javafx-') && it.name.endsWith('.jar') }
                .collect { it.absolutePath }
        if (!fxJars.isEmpty()) {
            jvmArgs += [
                    '--module-path', fxJars.join(File.pathSeparator),
                    '--add-modules', 'javafx.controls,javafx.graphics,javafx.fxml,javafx.web,javafx.base,javafx.swing,javafx.media'
            ]
        }
    }
}
```

**Notas de implementacion:**

- Se usa `doFirst` para resolver los ficheros en tiempo de ejecucion, no en tiempo de configuracion (evita problemas de orden de resolucion de dependencias).
- Se filtra por JARs cuyo nombre empieza por `javafx-` para construir el module-path exacto.
- `File.pathSeparator` garantiza compatibilidad multiplataforma (`:` en Linux/macOS, `;` en Windows).
- La tarea estandar `run` recibira los argumentos dos veces (del plugin y de este bloque), pero la JVM tolera argumentos duplicados sin problema.

### Criterio de aceptacion

- `./gradlew run` sigue funcionando igual que antes.
- Ejecutar `Main` desde IntelliJ (boton verde o Shift+F10) arranca la aplicacion sin errores de JavaFX.
- No aparece "JavaFX runtime components are missing" en ningun metodo de lanzamiento.

---

## FASE 2: Configuracion de ejecucion en IntelliJ

### Descripcion del problema

IntelliJ no tiene una Run Configuration guardada en `.idea/runConfigurations/`. Cuando el usuario pulsa "Run" sobre `Main.java`, IntelliJ crea automaticamente una tarea Gradle ad-hoc que no usa `./gradlew run`. Aunque la Fase 1 resuelve el problema tecnico, conviene tener una configuracion explicita para evitar confusiones.

### Solucion propuesta

Crear el directorio `.idea/runConfigurations/` y un fichero XML de configuracion Gradle que apunte a la tarea `run`:

```bash
mkdir -p .idea/runConfigurations
```

Crear el fichero `.idea/runConfigurations/Run_RehabiAPP.xml`:

```xml
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="Run RehabiAPP" type="GradleRunConfiguration" factoryName="Gradle">
    <ExternalSystemSettings>
      <option name="executionName" />
      <option name="externalProjectPath" value="$PROJECT_DIR$" />
      <option name="externalSystemIdString" value="GRADLE" />
      <option name="scriptParameters" value="" />
      <option name="taskDescriptions">
        <list />
      </option>
      <option name="taskNames">
        <list>
          <option value="run" />
        </list>
      </option>
      <option name="vmOptions" value="" />
    </ExternalSystemSettings>
    <GradleScriptDebugEnabled>true</GradleScriptDebugEnabled>
    <method v="2" />
  </configuration>
</component>
```

**Nota:** La carpeta `.idea/` normalmente esta en `.gitignore`, asi que cada desarrollador tendra que importarlo o crearlo. Alternativamente, se puede documentar en un comentario al inicio de `build.gradle`.

### Criterio de aceptacion

- Al abrir el proyecto en IntelliJ, aparece la configuracion "Run RehabiAPP" en el desplegable de Run Configurations.
- Seleccionar "Run RehabiAPP" y pulsar Run ejecuta `./gradlew run` correctamente.

---

## FASE 3: Limpieza de warnings de acceso nativo (opcional)

### Descripcion del problema

Java 24 muestra warnings como:
```
WARNING: A restricted operation was attempted
WARNING: Use --enable-native-access=ALL-UNNAMED to suppress
```

Estos provienen de librerias que usan APIs nativas restringidas (JavaFX internamente, JasperReports, etc.). Son warnings no fatales pero ensucian la salida de consola.

### Solucion propuesta

Añadir `--enable-native-access=ALL-UNNAMED` en dos lugares:

**1. En `tasks.withType(JavaExec).configureEach`**, junto con los demas `--add-opens`:

```groovy
jvmArgs += [
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
        "--add-opens=java.base/java.text=ALL-UNNAMED",
        "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
        "--add-opens=java.desktop/java.awt.geom=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--enable-native-access=ALL-UNNAMED"
]
```

**2. En `tasks.named('test')`**, junto con los demas argumentos:

```groovy
jvmArgs += [
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
        "--add-opens=java.base/java.text=ALL-UNNAMED",
        "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
        "-XX:+EnableDynamicAgentLoading",
        "-Xshare:off",
        "--enable-native-access=ALL-UNNAMED"
]
```

### Criterio de aceptacion

- `./gradlew run` no muestra warnings de native access.
- `./gradlew test` no muestra warnings de native access.
- Si algun argumento causa incompatibilidades, se revierte sin afectar las fases anteriores.

---

## FASE 4: Verificacion completa

### Descripcion del problema

Tras los cambios, hay que confirmar que ninguno de los tres metodos de lanzamiento se ha roto y que los tests siguen pasando.

### Solucion propuesta

Ejecutar en orden:

```bash
# 1. Tests unitarios (74 tests deben pasar)
./gradlew test

# 2. Lanzamiento via Gradle
./gradlew run
# Verificar: la ventana de login aparece, se puede interactuar, cerrar limpiamente.

# 3. Lanzamiento via JAR
./gradlew clean jar
java -jar build/libs/desktop.jar
# Verificar: la ventana de login aparece.

# 4. Lanzamiento via IntelliJ (configuracion guardada)
# Abrir IntelliJ > seleccionar "Run RehabiAPP" > pulsar Run
# Verificar: la ventana de login aparece sin errores de JavaFX.

# 5. Lanzamiento via IntelliJ (metodo clasico)
# Click derecho sobre Main.java > Run
# Verificar: gracias a la Fase 1, ahora tambien funciona sin errores.
```

### Criterio de aceptacion

- `./gradlew test` reporta 74 tests pasados, 0 fallidos.
- `./gradlew run` abre la aplicacion sin errores.
- `java -jar build/libs/desktop.jar` abre la aplicacion.
- IntelliJ Run Configuration "Run RehabiAPP" abre la aplicacion sin errores.
- IntelliJ Run directo sobre `Main.java` abre la aplicacion sin errores de JavaFX.
- No hay regresiones en funcionalidad existente.

---

## Resumen de ficheros a modificar

| Fichero | Fase | Cambio |
|---------|------|--------|
| `build.gradle` | 1 | Añadir bloque `doFirst` con `--module-path` y `--add-modules` dentro de `tasks.withType(JavaExec).configureEach` |
| `build.gradle` | 3 | Añadir `--enable-native-access=ALL-UNNAMED` a bloques `JavaExec` y `test` |
| `.idea/runConfigurations/Run_RehabiAPP.xml` | 2 | Crear fichero nuevo con configuracion Gradle `run` |

## Orden de ejecucion

Fase 1 es **critica** y resuelve el problema principal. Las fases 2, 3 y 4 son complementarias. El implementador debe ejecutarlas en orden numerico.
