#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

if [[ ! -f .env ]]; then
  echo "Arquivo backend/java/.env não encontrado. Copie .env.example para .env e configure as variáveis." >&2
  exit 1
fi

set -a
# .env é um arquivo local controlado pelo desenvolvedor; não inclua código nele.
source ./.env
set +a
exec mvn spring-boot:run
