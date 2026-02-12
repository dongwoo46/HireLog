#!/usr/bin/env bash
set -euo pipefail

POSTGRES_CONTAINER=pg_hirelog_dev
POSTGRES_USER=hirelog
POSTGRES_DB=hirelog_dev

DEBEZIUM_USER=debezium
DEBEZIUM_PASSWORD=debezium

CONNECT_URL="http://localhost:8083"
CONNECTOR_NAME="hirelog-outbox-connector"
SLOT_NAME="hirelog_outbox_slot"
PUBLICATION_NAME="dbz_publication"

OUTBOX_TABLES=("public.outbox_event")

echo "▶ [1/4] Verifying Postgres readiness..."

docker exec ${POSTGRES_CONTAINER} pg_isready -U "${POSTGRES_USER}" -d "${POSTGRES_DB}"

echo "▶ [2/4] Ensuring Debezium role & publication..."

docker exec -i ${POSTGRES_CONTAINER} psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} <<EOF

-- 1. Debezium role
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

-- 2. Publication
DO \$\$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_publication WHERE pubname = '${PUBLICATION_NAME}'
  ) THEN
    CREATE PUBLICATION ${PUBLICATION_NAME};
  END IF;
END
\$\$;

EOF

for TABLE in "${OUTBOX_TABLES[@]}"; do
  docker exec -i ${POSTGRES_CONTAINER} psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} <<EOF
DO \$\$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_publication_tables
    WHERE pubname = '${PUBLICATION_NAME}'
      AND schemaname || '.' || tablename = '${TABLE}'
  ) THEN
    ALTER PUBLICATION ${PUBLICATION_NAME} ADD TABLE ${TABLE};
  END IF;
END
\$\$;
EOF
done

echo "▶ [3/4] Waiting for Kafka Connect..."

until curl -s ${CONNECT_URL}/ > /dev/null; do
  sleep 2
done

echo "▶ [4/4] Upserting Debezium connector..."

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

    "publication.autocreate.mode": "disabled",
    "publication.name": "${PUBLICATION_NAME}",

    "table.include.list": "public.outbox_event",

    "snapshot.mode": "never",

    "tombstones.on.delete": "false",
    "decimal.handling.mode": "string",

    "heartbeat.interval.ms": "10000",
    "errors.tolerance": "all",
    "errors.log.enable": "true",
    "errors.log.include.messages": "true",

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

if curl -s ${CONNECT_URL}/connectors/${CONNECTOR_NAME} > /dev/null; then
  echo "  Updating existing connector..."
  curl -s -X PUT \
    -H "Content-Type: application/json" \
    ${CONNECT_URL}/connectors/${CONNECTOR_NAME}/config \
    -d "$(echo ${CONNECTOR_CONFIG} | jq '.config')" > /dev/null
else
  echo "  Creating new connector..."
  curl -s -X POST \
    -H "Content-Type: application/json" \
    ${CONNECT_URL}/connectors \
    -d "${CONNECTOR_CONFIG}" > /dev/null
fi

sleep 3

STATUS=$(curl -s ${CONNECT_URL}/connectors/${CONNECTOR_NAME}/status)
CONNECTOR_STATE=$(echo "$STATUS" | jq -r '.connector.state')
TASK_STATE=$(echo "$STATUS" | jq -r '.tasks[0].state')

echo ""
echo "============================================"
echo " CDC Setup Complete"
echo " connector: ${CONNECTOR_STATE}"
echo " task     : ${TASK_STATE}"
echo "============================================"
