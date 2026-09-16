#!/bin/bash
set -euo pipefail

ENDPOINT_URL="${DYNAMODB_ENDPOINT_URL:-http://dynamodb:8000}"
REGION="${AWS_DEFAULT_REGION:-us-east-1}"
ACCOUNT_TABLE_NAME="${DYNAMODB_ACCOUNT_TABLE_NAME:-accounts}"
TRANSACTION_TABLE_NAME="${DYNAMODB_TRANSACTION_TABLE_NAME:-transactions}"
IDEMPOTENCY_TABLE_NAME="${DYNAMODB_TRANSACTION_IDEMPOTENCY_TABLE_NAME:-transaction_idempotency}"
IDEMPOTENCY_TTL_SECONDS="${TRANSACTION_IDEMPOTENCY_TTL_SECONDS:-86400}"
TRANSACTION_ID="8e8ae808-b154-48b5-9f3e-553935cc4543"

echo "Waiting for DynamoDB Local at ${ENDPOINT_URL}..."
until aws dynamodb list-tables --endpoint-url "${ENDPOINT_URL}" --region "${REGION}" >/dev/null 2>&1; do
  echo "  not ready yet, retrying in 2s..."
  sleep 2
done
echo "DynamoDB Local is ready."

create_account_table() {
  aws dynamodb create-table \
    --table-name "${ACCOUNT_TABLE_NAME}" \
    --attribute-definitions AttributeName=id,AttributeType=S \
    --key-schema AttributeName=id,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --endpoint-url "${ENDPOINT_URL}" \
    --region "${REGION}" >/dev/null
}

create_transaction_table() {
  aws dynamodb create-table \
    --table-name "${TRANSACTION_TABLE_NAME}" \
    --attribute-definitions AttributeName=accountId,AttributeType=S AttributeName=timestamp,AttributeType=N \
    --key-schema AttributeName=accountId,KeyType=HASH AttributeName=timestamp,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --endpoint-url "${ENDPOINT_URL}" \
    --region "${REGION}" >/dev/null
}

create_idempotency_table() {
  aws dynamodb create-table \
    --table-name "${IDEMPOTENCY_TABLE_NAME}" \
    --attribute-definitions AttributeName=id,AttributeType=S \
    --key-schema AttributeName=id,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --endpoint-url "${ENDPOINT_URL}" \
    --region "${REGION}" >/dev/null
}

ensure_table() {
  local table_name="$1"
  local create_function="$2"

  if aws dynamodb describe-table --table-name "${table_name}" --endpoint-url "${ENDPOINT_URL}" --region "${REGION}" >/dev/null 2>&1; then
    echo "Table '${table_name}' already exists, skipping creation."
    return
  fi

  echo "Creating table '${table_name}'..."
  "${create_function}"
  aws dynamodb wait table-exists \
    --table-name "${table_name}" \
    --endpoint-url "${ENDPOINT_URL}" \
    --region "${REGION}"
  echo "Table '${table_name}' created."
}

ensure_table "${ACCOUNT_TABLE_NAME}" create_account_table
ensure_table "${TRANSACTION_TABLE_NAME}" create_transaction_table
ensure_table "${IDEMPOTENCY_TABLE_NAME}" create_idempotency_table

TTL_STATUS=$(aws dynamodb describe-time-to-live \
  --table-name "${IDEMPOTENCY_TABLE_NAME}" \
  --endpoint-url "${ENDPOINT_URL}" \
  --region "${REGION}" \
  --query 'TimeToLiveDescription.TimeToLiveStatus' \
  --output text 2>/dev/null || true)

if [[ "${TTL_STATUS}" != "ENABLED" && "${TTL_STATUS}" != "ENABLING" ]]; then
  aws dynamodb update-time-to-live \
    --table-name "${IDEMPOTENCY_TABLE_NAME}" \
    --time-to-live-specification Enabled=true,AttributeName=expiresAt \
    --endpoint-url "${ENDPOINT_URL}" \
    --region "${REGION}" >/dev/null
fi

echo "Seeding balance account and transaction..."
aws dynamodb put-item \
  --table-name "${ACCOUNT_TABLE_NAME}" \
  --item file:///dynamodb-seed/account-seed.json \
  --endpoint-url "${ENDPOINT_URL}" \
  --region "${REGION}" >/dev/null

aws dynamodb put-item \
  --table-name "${TRANSACTION_TABLE_NAME}" \
  --item file:///dynamodb-seed/transaction-seed.json \
  --endpoint-url "${ENDPOINT_URL}" \
  --region "${REGION}" >/dev/null

EXPIRES_AT=$(( $(date +%s) + IDEMPOTENCY_TTL_SECONDS ))
IDEMPOTENCY_ITEM=$(printf '{"id":{"S":"%s"},"expiresAt":{"N":"%s"}}' "${TRANSACTION_ID}" "${EXPIRES_AT}")
aws dynamodb put-item \
  --table-name "${IDEMPOTENCY_TABLE_NAME}" \
  --item "${IDEMPOTENCY_ITEM}" \
  --endpoint-url "${ENDPOINT_URL}" \
  --region "${REGION}" >/dev/null

echo "Seed complete for '${ACCOUNT_TABLE_NAME}', '${TRANSACTION_TABLE_NAME}' and '${IDEMPOTENCY_TABLE_NAME}'."
