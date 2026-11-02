#!/usr/bin/env bash
# Compila y levanta la app web (API + frontend). Requiere JDK 21 (ver pom.xml).
#   ./run.sh [puerto]       sin .env -> datos de ejemplo en memoria; con DB_URL en .env -> Supabase/Postgres
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name '*.java')
exec java -Dstdout.encoding=UTF-8 -cp "out:$(ls ~/.m2/repository/org/postgresql/postgresql/*/postgresql-*.jar 2>/dev/null | head -1)" MainWeb "${1:-8080}" web
