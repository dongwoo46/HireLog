#!/usr/bin/env bash
set -euo pipefail

# ─────────────────────────────────────────────
# 1. Load .env.prod with auto-export
# ─────────────────────────────────────────────

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
ENV_FILE="${SCRIPT_DIR}/.env.prod"

if [ ! -f "$ENV_FILE" ]; then
  echo "❌ .env.prod not found at $ENV_FILE"
  exit 1
fi

set -a
source "$ENV_FILE"
set +a

# ─────────────────────────────────────────────
# 2. Validate required environment variables
# ─────────────────────────────────────────────

: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is not set in .env.prod}"
: "${POSTGRES_USER:?POSTGRES_USER is not set in .env.prod}"
: "${POSTGRES_DB:?POSTGRES_DB is not set in .env.prod}"

POSTGRES_CONTAINER=pg_prod
DEBEZIUM_USER=debezium
DEBEZIUM_PASSWORD=debezium

# debezium_prod 는 expose 만 선언(ports 미사용) → host에서 localhost:8083 접근 불가
# docker inspect 로 컨테이너 내부 IP 를 가져와 직접 접근
DEBEZIUM_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' debezium_prod)
CONNECT_URL="http://${DEBEZIUM_IP}:8083"

CONNECTOR_NAME="hirelog-outbox-connector"
SLOT_NAME="hirelog_outbox_slot"
PUBLICATION_NAME="dbz_publication"
OUTBOX_TABLE="public.outbox_event"

echo "============================================"
echo " Loaded Environment Variables"
echo " POSTGRES_USER=$POSTGRES_USER"
echo " POSTGRES_DB=$POSTGRES_DB"
echo " CONNECT_URL=$CONNECT_URL"
echo "============================================"

# ─────────────────────────────────────────────
# 3. Postgres readiness check
# ─────────────────────────────────────────────

echo "▶ [1/4] Verifying Postgres readiness..."

docker exec ${POSTGRES_CONTAINER} \
  pg_isready -U "${POSTGRES_USER}" -d "${POSTGRES_DB}"

# ─────────────────────────────────────────────
# 4. Create Debezium role + publication
# ─────────────────────────────────────────────

echo "▶ [2/4] Ensuring Debezium role & publication..."

docker exec -e PGPASSWORD="${POSTGRES_PASSWORD}" -i ${POSTGRES_CONTAINER} \
psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" <<EOF

DO \$\$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${DEBEZIUM_USER}') THEN
    CREATE ROLE ${DEBEZIUM_USER}
      WITH LOGIN REPLICATION PASSWORD '${DEBEZIUM_PASSWORD}';
  END IF;
END
\$\$;

GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO ${DEBEZIUM_USER};
GRANT USAGE ON SCHEMA public TO ${DEBEZIUM_USER};
GRANT SELECT ON TABLE ${OUTBOX_TABLE} TO ${DEBEZIUM_USER};

DO \$\$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_publication WHERE pubname = '${PUBLICATION_NAME}'
  ) THEN
    CREATE PUBLICATION ${PUBLICATION_NAME};
  END IF;
END
\$\$;

ALTER PUBLICATION ${PUBLICATION_NAME} ADD TABLE ${OUTBOX_TABLE};

EOF

# ─────────────────────────────────────────────
# 5. Wait for Kafka Connect
# ─────────────────────────────────────────────

echo "▶ [3/4] Waiting for Kafka Connect..."

until curl -s ${CONNECT_URL}/ > /dev/null; do
  sleep 2
done

# ─────────────────────────────────────────────
# 6. Create or update connector
# ─────────────────────────────────────────────

echo "▶ [4/4] Creating or Updating Connector..."

CONNECTOR_CONFIG=$(cat <<JSON
{
  "name": "${CONNECTOR_NAME}",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.hostname": "postgres",
    "database.port": "5432",
    "database.user": "${DEBEZIUM_USER}",
    "database.password": "${DEBEZIUM_PASSWORD}",
    "database.dbname": "${POSTGRES_DB}",

    "topic.prefix": "hirelog",

    "plugin.name": "pgoutput",
    "slot.name": "${SLOT_NAME}",
    "publication.name": "${PUBLICATION_NAME}",
    "publication.autocreate.mode": "disabled",

    "table.include.list": "${OUTBOX_TABLE}",
    "snapshot.mode": "never",

    "tombstones.on.delete": "false",
    "decimal.handling.mode": "string",

    "heartbeat.interval.ms": "10000",

    "transforms": "outbox",
    "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
    "transforms.outbox.table.field.event.id": "id",
    "transforms.outbox.table.field.event.key": "aggregate_id",
    "transforms.outbox.table.field.event.type": "event_type",
    "transforms.outbox.table.field.event.payload": "payload",
    "transforms.outbox.route.by.field": "aggregate_type",
    "transforms.outbox.route.topic.replacement": "hirelog.outbox.\${routedByValue}",

    "key.converter": "org.apache.kafka.connect.storage.StringConverter",
    "value.converter": "org.apache.kafka.connect.json.JsonConverter",
    "value.converter.schemas.enable": "false"
  }
}
JSON
)

HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  ${CONNECT_URL}/connectors/${CONNECTOR_NAME})

if [ "$HTTP_STATUS" -eq 200 ]; then
  echo "Updating existing connector..."
  RESPONSE=$(curl -s -X PUT \
    -H "Content-Type: application/json" \
    ${CONNECT_URL}/connectors/${CONNECTOR_NAME}/config \
    -d "$(echo ${CONNECTOR_CONFIG} | jq '.config')")
else
  echo "Creating new connector..."
  RESPONSE=$(curl -s -X POST \
    -H "Content-Type: application/json" \
    ${CONNECT_URL}/connectors \
    -d "${CONNECTOR_CONFIG}")
fi

echo "Connector Response:"
echo "$RESPONSE"

sleep 3

STATUS=$(curl -s ${CONNECT_URL}/connectors/${CONNECTOR_NAME}/status || true)

echo ""
echo "============================================"
echo "COMPLETED Debezium Connector"
echo "============================================"
