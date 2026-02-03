@echo off
REM ================================================================================
REM Lanzador para REHABIAPP
REM Sistema de Gestión de Expedientes de Rehabilitación
REM ================================================================================

REM Verificar que existe el JAR principal
if not exist "REHABIAPP.jar" (
    echo ERROR: No se encuentra REHABIAPP.jar
    echo Asegurate de estar en el directorio correcto
    pause
    exit /b 1
)

REM Verificar que existe la carpeta lib con las dependencias
if not exist "lib\" (
    echo ERROR: No se encuentra la carpeta lib con las dependencias
    echo Asegurate de copiar tanto REHABIAPP.jar como la carpeta lib/
    pause
    exit /b 1
)

echo ================================================================================
echo Iniciando REHABIAPP...
echo ================================================================================
echo.

REM Ejecutar la aplicación con los argumentos JVM necesarios
java ^
    --add-opens=java.base/java.lang=ALL-UNNAMED ^
    --add-opens=java.base/java.util=ALL-UNNAMED ^
    --add-opens=java.base/java.lang.reflect=ALL-UNNAMED ^
    --add-opens=java.base/java.text=ALL-UNNAMED ^
    --add-opens=java.desktop/java.awt.font=ALL-UNNAMED ^
    --add-opens=java.desktop/java.awt.geom=ALL-UNNAMED ^
    --add-opens=java.base/java.io=ALL-UNNAMED ^
    -jar REHABIAPP.jar

REM Capturar código de salida
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ================================================================================
    echo ERROR: La aplicacion termino con errores ^(codigo %ERRORLEVEL%^)
    echo ================================================================================
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ================================================================================
echo REHABIAPP cerrado correctamente
echo ================================================================================
