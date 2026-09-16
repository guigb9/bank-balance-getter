#!/bin/bash
set -euo pipefail

BROKERS="${REDPANDA_BROKERS:-redpanda:9092}"

FINANCIAL_TOPIC_NAME="${FINANCIAL_TRANSACTIONS_TOPIC:-transacoes-financeiras-processadas}"
SEED_FILE="/redpanda-seed/financial-transactions-seed.jsonl"

echo "Waiting for Redpanda broker at ${BROKERS}..."
until rpk cluster info --brokers "${BROKERS}" >/dev/null 2>&1; do
  echo "  not ready yet, retrying in 2s..."
  sleep 2
done
echo "Redpanda broker is ready."

if rpk topic describe "${FINANCIAL_TOPIC_NAME}" --brokers "${BROKERS}" >/dev/null 2>&1; then
  echo "Resetting topic '${FINANCIAL_TOPIC_NAME}'..."
  rpk topic delete "${FINANCIAL_TOPIC_NAME}" --brokers "${BROKERS}" >/dev/null
  until ! rpk topic describe "${FINANCIAL_TOPIC_NAME}" --brokers "${BROKERS}" >/dev/null 2>&1; do
    sleep 1
  done
fi

echo "Creating topic '${FINANCIAL_TOPIC_NAME}'..."
rpk topic create "${FINANCIAL_TOPIC_NAME}" --brokers "${BROKERS}" --partitions 1 --replicas 1

echo "Publishing seed messages from ${SEED_FILE}..."
rpk topic produce "${FINANCIAL_TOPIC_NAME}" --brokers "${BROKERS}" -f '%v\n' < "${SEED_FILE}"

COUNT=$(wc -l < "${SEED_FILE}" | tr -d ' ')
echo "Seed complete. Published ${COUNT} message(s) to '${FINANCIAL_TOPIC_NAME}'."
