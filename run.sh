#!/usr/bin/env sh
set -eu

cd "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"

if ! command -v java >/dev/null 2>&1; then
  echo "Java nao foi encontrado no PATH. Instale o Java 21 ou superior e tente novamente." >&2
  exit 1
fi

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven nao foi encontrado no PATH. Instale o Maven e tente novamente." >&2
  exit 1
fi

app_port="${SERVER_PORT:-8080}"
app_url="http://127.0.0.1:${app_port}/"

open_browser() {
  if command -v xdg-open >/dev/null 2>&1; then
    xdg-open "$app_url" >/dev/null 2>&1 &
  elif command -v open >/dev/null 2>&1; then
    open "$app_url"
  elif command -v powershell.exe >/dev/null 2>&1; then
    powershell.exe -NoProfile -Command "Start-Process '$app_url'" >/dev/null 2>&1
  else
    echo "Abra $app_url no navegador."
  fi
}

echo "Iniciando Anytype View em $app_url"
mvn spring-boot:run &
app_pid=$!

attempt=1
while [ "$attempt" -le 90 ]; do
  if curl --fail --silent --show-error --max-time 1 "$app_url" >/dev/null 2>&1; then
    open_browser
    echo "Aplicacao em execucao. Use Ctrl+C para encerra-la."
    wait "$app_pid"
    exit $?
  fi

  if ! kill -0 "$app_pid" 2>/dev/null; then
    echo "A aplicacao foi encerrada durante a inicializacao." >&2
    exit 1
  fi

  sleep 1
  attempt=$((attempt + 1))
done

echo "A aplicacao nao respondeu em 90 segundos." >&2
exit 1
