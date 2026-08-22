@echo off
setlocal

cd /d "%~dp0"

where java >nul 2>nul
if errorlevel 1 (
    echo Java nao foi encontrado no PATH. Instale o Java 21 ou superior e tente novamente.
    exit /b 1
)

where mvn >nul 2>nul
if errorlevel 1 (
    echo Maven nao foi encontrado no PATH. Instale o Maven e tente novamente.
    exit /b 1
)

if "%SERVER_PORT%"=="" (
    set "APP_PORT=8080"
) else (
    set "APP_PORT=%SERVER_PORT%"
)

set "APP_URL=http://127.0.0.1:%APP_PORT%/"

echo Iniciando Anytype View em %APP_URL%
start "Anytype View" cmd /k "mvn spring-boot:run"

echo Aguardando a aplicacao ficar disponivel...
powershell -NoProfile -Command "$url = '%APP_URL%'; for ($attempt = 1; $attempt -le 90; $attempt++) { try { $response = Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec 1 -ErrorAction Stop; Start-Process $url; exit 0 } catch { Start-Sleep -Seconds 1 } }; exit 1"
if errorlevel 1 (
    echo A aplicacao nao respondeu em 90 segundos. Abra %APP_URL% manualmente quando ela concluir a inicializacao.
    exit /b 1
)
echo O navegador foi aberto.
endlocal
