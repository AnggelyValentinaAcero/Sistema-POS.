@echo off
REM ======================================================================
REM  Genera el ejecutable de Windows del Sistema POS con jpackage (viene con el JDK).
REM
REM  Uso:
REM    scripts\crear_ejecutable.bat        -> carpeta portable dist\SistemaPOS\SistemaPOS.exe
REM    scripts\crear_ejecutable.bat exe    -> instalador dist\SistemaPOS-1.0.0.exe
REM                                           (requiere WiX Toolset 3 instalado)
REM
REM  El resultado incluye su propio Java: la computadora donde se use
REM  no necesita tener Java instalado.
REM ======================================================================

cd /d "%~dp0.."

set VERSION=1.0.0
set JAR=sistema-pos-0.1.0-SNAPSHOT.jar

REM Buscar jpackage (está en la carpeta bin del JDK), en este orden:
REM   1. en JAVA_HOME   2. en el PATH   3. en los JDK instalados en Program Files\Java
set JPACKAGE=
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jpackage.exe" set JPACKAGE="%JAVA_HOME%\bin\jpackage.exe"
if not defined JPACKAGE (
    where jpackage >nul 2>nul
    if not errorlevel 1 set JPACKAGE=jpackage
)
if not defined JPACKAGE (
    REM si hay varios JDK, queda el último en orden alfabético (ej: jdk-25 después de jdk-17)
    for /d %%D in ("%ProgramFiles%\Java\jdk-*") do (
        if exist "%%D\bin\jpackage.exe" set JPACKAGE="%%D\bin\jpackage.exe"
    )
)
if not defined JPACKAGE (
    echo No se encontro jpackage. Instale un JDK 21 o superior, o defina JAVA_HOME.
    exit /b 1
)
echo Usando %JPACKAGE%

echo [1/3] Compilando el proyecto...
call "%~dp0..\mvnw.cmd" -q clean package -DskipTests
if errorlevel 1 (
    echo Error al compilar.
    exit /b 1
)

echo [2/3] Preparando archivos...
if exist dist rmdir /s /q dist
mkdir dist\entrada
copy target\%JAR% dist\entrada\ >nul

REM Opciones para armar el Java reducido que va dentro del ejecutable.
REM --bind-services agrega los modulos "proveedores de servicios" que jpackage omite por defecto:
REM   jdk.charsets   (codificaciones: sin el, el driver de MySQL corrompe tildes y enies)
REM   jdk.localedata (formatos de fecha y numero en espanol)
REM   jdk.crypto.*   (cifrado de la conexion)
set JLINK_OPCIONES=--strip-native-commands --strip-debug --no-man-pages --no-header-files --bind-services

echo [3/3] Generando el ejecutable con jpackage...
if /i "%1"=="exe" (
    %JPACKAGE% --type exe --name SistemaPOS --app-version %VERSION% --vendor "Proyecto POS" ^
        --input dist\entrada --main-jar %JAR% --main-class com.pos.app.Launcher ^
        --java-options "--enable-native-access=ALL-UNNAMED" ^
        --jlink-options "%JLINK_OPCIONES%" ^
        --win-shortcut --win-menu --win-dir-chooser --dest dist
) else (
    %JPACKAGE% --type app-image --name SistemaPOS --app-version %VERSION% --vendor "Proyecto POS" ^
        --input dist\entrada --main-jar %JAR% --main-class com.pos.app.Launcher ^
        --java-options "--enable-native-access=ALL-UNNAMED" ^
        --jlink-options "%JLINK_OPCIONES%" ^
        --dest dist
)
if errorlevel 1 (
    echo Error al ejecutar jpackage. Verifique que JAVA_HOME apunte a un JDK 21 o superior.
    exit /b 1
)

rmdir /s /q dist\entrada
echo.
echo Listo. Resultado en la carpeta dist\
