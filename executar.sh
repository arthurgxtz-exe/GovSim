#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

echo "======================================================================"
echo "  GOVSIM - SIMULADOR DE GESTAO GOVERNAMENTAL EM TURNOS"
echo "  Inicializando compilacao e servidor local..."
echo "======================================================================"

mkdir -p bin/govsim/engine
cp src/govsim/engine/eventos.json bin/govsim/engine/eventos.json

if command -v javac >/dev/null 2>&1; then
    echo "[INFO] Compilando codigo-fonte Java..."
    if javac -encoding UTF-8 --release 8 -Xlint:-options -d bin src/govsim/model/*.java src/govsim/engine/*.java src/govsim/server/*.java src/govsim/Main.java 2>/dev/null; then
        echo "[OK] Compilado com sucesso (compatibilidade Java 8+)."
    else
        javac -encoding UTF-8 -source 8 -target 8 -d bin src/govsim/model/*.java src/govsim/engine/*.java src/govsim/server/*.java src/govsim/Main.java
        echo "[OK] Compilado com sucesso."
    fi
else
    echo "[AVISO] javac nao encontrado; utilizando classes pre-compiladas em ./bin"
fi

echo "[INFO] Iniciando servidor HTTP GovSim..."
exec java -cp bin govsim.Main "$@"
