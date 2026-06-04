@REM Maven Wrapper script for Windows
@REM Downloads Maven 3.9.8 on first run and caches it in %USERPROFILE%\.m2\wrapper\dists\

@echo off
setlocal

set MAVEN_VERSION=3.9.8
set WRAPPER_DIR=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%
set MAVEN_HOME=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%
set MVN_CMD=%MAVEN_HOME%\bin\mvn.cmd
set DOWNLOAD_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip

if exist "%MVN_CMD%" goto run_maven

echo [mvnw] Maven %MAVEN_VERSION% не е намерен. Изтегляне...

if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"

powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%WRAPPER_DIR%\maven.zip'"
if errorlevel 1 (
    echo [mvnw] Грешка при изтегляне на Maven!
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%WRAPPER_DIR%\maven.zip' -DestinationPath '%WRAPPER_DIR%' -Force"
if errorlevel 1 (
    echo [mvnw] Грешка при разархивиране!
    exit /b 1
)

del "%WRAPPER_DIR%\maven.zip"
echo [mvnw] Maven %MAVEN_VERSION% е инсталиран успешно.

:run_maven
"%MVN_CMD%" %*
