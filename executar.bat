@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo ======================================================================
echo   GOVSIM - SIMULADOR DE GESTAO GOVERNAMENTAL EM TURNOS
echo   Inicializando compilacao e servidor local...
echo ======================================================================

if not exist "bin" mkdir "bin"
if not exist "bin\govsim\engine" mkdir "bin\govsim\engine"
copy /Y "src\govsim\engine\eventos.json" "bin\govsim\engine\eventos.json" >nul
if errorlevel 1 (
    echo [ERRO] Nao foi possivel atualizar eventos.json em bin.
    exit /b 1
)

set "JAVAC_CMD="
where javac >nul 2>nul
if %errorlevel% equ 0 (
    set "JAVAC_CMD=javac"
)

if "!JAVAC_CMD!"=="" (
    if defined JAVA_HOME (
        if exist "%JAVA_HOME%\bin\javac.exe" (
            set "JAVAC_CMD=%JAVA_HOME%\bin\javac.exe"
        )
    )
)

if "!JAVAC_CMD!"=="" (
    for /d %%D in ("%USERPROFILE%\.vscode\extensions\redhat.java*") do (
        for /d %%J in ("%%D\jre\*") do (
            if exist "%%J\bin\javac.exe" (
                set "JAVAC_CMD=%%J\bin\javac.exe"
            )
        )
    )
)

if not "!JAVAC_CMD!"=="" (
    echo [INFO] Compilador Java localizado: "!JAVAC_CMD!"
    "!JAVAC_CMD!" -encoding UTF-8 --release 8 -Xlint:-options -d bin src\govsim\model\*.java src\govsim\engine\*.java src\govsim\server\*.java src\govsim\Main.java 2>nul
    if !errorlevel! neq 0 (
        "!JAVAC_CMD!" -encoding UTF-8 -source 8 -target 8 -d bin src\govsim\model\*.java src\govsim\engine\*.java src\govsim\server\*.java src\govsim\Main.java
    )
    if !errorlevel! neq 0 (
        echo [ERRO] Compilacao falhou. O jogo nao sera iniciado com classes antigas.
        exit /b 1
    )
    echo [OK] Codigo Java compilado com sucesso em .\bin
) else (
    echo [AVISO] javac nao encontrado no PATH; utilizando classes pre-compiladas em .\bin
)

if not exist "bin\govsim\Main.class" (
    echo [ERRO] Nao foi possivel localizar os arquivos compilados em bin\govsim\Main.class.
    pause
    exit /b 1
)

echo [INFO] Iniciando servidor HTTP GovSim...
java -cp bin govsim.Main %*
if %errorlevel% neq 0 (
    echo.
    echo [ERRO] A execucao do GovSim foi encerrada com codigo de erro.
    pause
)
