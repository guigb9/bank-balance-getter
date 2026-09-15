#!/bin/sh
# Alterado para /bin/sh para máxima compatibilidade no Docker Alpine

# Em shell comum (sh/ash), habilitamos o 'e' (parar em erros) e 'u' (erro em variáveis não declaradas).
# O 'pipefail' foi removido porque o interpretador padrão não dá suporte a ele.
set -eu

TOPIC="${1:?Usage: produce-transactions-events.sh <topic> [count]}"
COUNT="${2:-100}"
BROKERS="${REDPANDA_BROKERS:-redpanda:9092}"
WINDOW_10MIN_US=$((10 * 60 * 1000000))
WINDOW_10YEARS_US=$((10 * 365 * 24 * 60 * 60 * 1000000))
NOW_US=$(date +%s%6N)

random_choice() {
  # O operador (( )) é exclusivo do Bash. Mudamos para sintaxe universal do POSIX sh.
  if [ $((RANDOM % 2)) -eq 0 ]; then echo "$1"; else echo "$2"; fi
}

random_amount() {
  awk -v min="$1" -v max="$2" -v seed="$RANDOM$RANDOM" 'BEGIN { srand(seed); printf "%.2f", min + rand() * (max - min) }'
}

random_offset_us() {
  local window=$1
  # O operador << também é exclusivo do Bash. Mudamos para aritmética tradicional equivalente.
  local wide=$(( RANDOM + (RANDOM * 32768) + (RANDOM * 1073741824) ))
  echo $(( wide % window ))
}

echo "Producing ${COUNT} transaction event(s) to topic '${TOPIC}' (brokers: ${BROKERS})..."

# O laço 'for ((...))' é sintaxe do Bash. Mudamos para o laço 'while' compatível com o 'sh' do Alpine.
i=1
while [ "$i" -le "$COUNT" ]; do
  tx_timestamp=$((NOW_US - $(random_offset_us "$WINDOW_10MIN_US")))
  account_created_at=$((NOW_US - $(random_offset_us "$WINDOW_10YEARS_US")))

  printf '{"transaction": {"id": "%s", "type": "%s", "amount": %s, "currency": "BRL", "status": "%s", "timestamp": %s}, "account": {"id": "%s", "owner": "%s", "created_at": %s, "status": "ENABLED", "balance": {"amount": %s, "currency": "BRL"}}}\n' \
    "$(cat /proc/sys/kernel/random/uuid)" \
    "$(random_choice CREDIT DEBIT)" \
    "$(random_amount 0.01 10000)" \
    "$(random_choice APPROVED DECLINED)" \
    "$tx_timestamp" \
    "$(cat /proc/sys/kernel/random/uuid)" \
    "$(cat /proc/sys/kernel/random/uuid)" \
    "$account_created_at" \
    "$(random_amount 0.00 20000)"

  i=$((i + 1))
done | rpk topic produce "${TOPIC}" --brokers "${BROKERS}" -f '%v\n'

echo "Done. Published ${COUNT} event(s) to '${TOPIC}'."
