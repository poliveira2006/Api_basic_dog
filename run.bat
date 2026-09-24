@echo off
setlocal

REM ============================================
REM  Canil - runner
REM  Compila se necessario e sobe o servidor
REM ============================================

cd /d "%~dp0"

set SRC_DIR=src
set BIN_DIR=bin
set MAIN=App
set PORT=8080

REM --- checa Java ---
where javac >nul 2>nul
if errorlevel 1 (
    echo.
    echo [erro] javac nao encontrado no PATH.
    echo Instale o JDK ^(17 ou superior^) e tente novamente.
    echo.
    pause
    exit /b 1
)

REM --- cria bin/ se nao existir ---
if not exist "%BIN_DIR%" mkdir "%BIN_DIR%"

REM --- compila se algum .java for mais novo que o .class ---
set NEED_COMPILE=0

if not exist "%BIN_DIR%\%MAIN%.class" (
    set NEED_COMPILE=1
) else (
    for /r "%SRC_DIR%" %%F in (*.java) do (
        if not exist "%BIN_DIR%\%%~nF.class" set NEED_COMPILE=1
    )
)

if "%NEED_COMPILE%"=="1" (
    echo [1/2] Compilando...
    del /q "%BIN_DIR%\*.class" >nul 2>nul

    dir /s /b "%SRC_DIR%\*.java" > "%TEMP%\canil_sources.txt"

    javac -encoding UTF-8 -d "%BIN_DIR%" "@%TEMP%\canil_sources.txt"
    if errorlevel 1 (
        echo.
        echo [erro] Falha na compilacao.
        del /q "%TEMP%\canil_sources.txt" >nul 2>nul
        pause
        exit /b 1
    )

    del /q "%TEMP%\canil_sources.txt" >nul 2>nul
    echo       ok
) else (
    echo [1/2] Nada para compilar.
)

REM --- abre o navegador depois de 1s ---
echo [2/2] Iniciando servidor em http://localhost:%PORT%
start "" /b cmd /c "timeout /t 1 >nul & start http://localhost:%PORT%"

REM --- roda (bloqueia ate Ctrl+C) ---
java -cp "%BIN_DIR%" %MAIN%

echo.
echo Servidor encerrado.
pause
endlocal