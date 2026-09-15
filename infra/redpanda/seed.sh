#!/bin/bash
set -euo pipefail

BROKERS="${REDPANDA_BROKERS:-redpanda:9092}"
TOPIC_NAME="${GREETING_TEMPLATES_TOPIC:-greeting-templates}"
FINANCIAL_TOPIC_NAME="${FINANCIAL_TRANSACTIONS_TOPIC:-transacoes-financeiras-processadas}"
SEED_FILE="/redpanda-seed/greeting-templates-seed.jsonl"

echo "Waiting for Redpanda broker at ${BROKERS}..."
until rpk cluster info --brokers "${BROKERS}" >/dev/null 2>&1; do
  echo "  not ready yet, retrying in 2s..."
  sleep 2
done
echo "Redpanda broker is ready."

create_topic() {
  local topic="$1"
  if rpk topic describe "${topic}" --brokers "${BROKERS}" >/dev/null 2>&1; then
    echo "Topic '${topic}' already exists, skipping creation."
  else
    echo "Creating topic '${topic}'..."
    rpk topic create "${topic}" --brokers "${BROKERS}" --partitions 1 --replicas 1
  fi
}

create_topic "${TOPIC_NAME}"
create_topic "${FINANCIAL_TOPIC_NAME}"

echo "Publishing seed messages from ${SEED_FILE}..."
rpk topic produce "${TOPIC_NAME}" --brokers "${BROKERS}" -f '%v\n' < "${SEED_FILE}"

COUNT=$(wc -l < "${SEED_FILE}" | tr -d ' ')
echo "Seed complete. Published ${COUNT} message(s) to '${TOPIC_NAME}'."
