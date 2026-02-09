#!/usr/bin/env bash
set -e

POSTGRES_CONTAINER=pg_hirelog
POSTGRES_USER=postgres
POSTGRES_DB=hirelog
DEBEZIUM_USER=debezium
DEBEZIUM_PASSWORD=debezium

CONNECT_URL="http://localhost:8083"
CONNECTOR_NAME="hirelog-outbox-connector"

# CDC 대상 outbox 테이블 목록
OUTBOX_TABLES=(
  "public.outbox_event"
  # "public.audit_outbox_event"
  # "public.outbox_event_v2"
)

echo "▶ [1/5] Updating postgresql.conf..."
docker exec -i ${POSTGRES_CONTAINER} bash <<EOF
set -e

CONF_FILE=/var/lib/postgresql/data/postgresql.conf
HBA_FILE=/var/lib/postgresql/data/pg_hba.conf

sed -i "s/^#*wal_level.*/wal_level = logical/" \$CONF_FILE
sed -i "s/^#*max_replication_slots.*/max_replication_slots = 10/" \$CONF_FILE
sed -i "s/^#*max_wal_senders.*/max_wal_senders = 10/" \$CONF_FILE

grep -q "host replication ${DEBEZIUM_USER}" \$HBA_FILE || \
  echo "host replication ${DEBEZIUM_USER} 0.0.0.0/0 md5" >> \$HBA_FILE

grep -q "host ${POSTGRES_DB} ${DEBEZIUM_USER}" \$HBA_FILE || \
  echo "host ${POSTGRES_DB} ${DEBEZIUM_USER} 0.0.0.0/0 md5" >> \$HBA_FILE
EOF

echo "▶ [2/5] Restarting Postgres..."
docker restart ${POSTGRES_CONTAINER}

for i in {1..30}; do
  if docker exec ${POSTGRES_CONTAINER} pg_isready -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" > /dev/null 2>&1; then
    echo "  Postgres is ready"
    break
  fi
  if [ $i -eq 30 ]; then
    echo "  ERROR: Postgres did not start within 30s"
    exit 1
  fi
  sleep 1
done

echo "▶ [3/5] Creating Debezium user & grants..."
docker exec -i ${POSTGRES_CONTAINER} \
  psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} <<EOF

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

EOF

echo "▶ [4/5] Ensuring publication & outbox tables..."
docker exec -i ${POSTGRES_CONTAINER} \
  psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} <<EOF

DO \$\$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_publication WHERE pubname = 'dbz_publication'
  ) THEN
    CREATE PUBLICATION dbz_publication;
  END IF;
END
\$\$;

EOF

for TABLE in "${OUTBOX_TABLES[@]}"; do
  echo "  Adding table: ${TABLE}"
  docker exec -i ${POSTGRES_CONTAINER} \
    psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} <<EOF
DO \$\$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_publication_tables
    WHERE pubname = 'dbz_publication'
      AND schemaname || '.' || tablename = '${TABLE}'
  ) THEN
    ALTER PUBLICATION dbz_publication ADD TABLE ${TABLE};
  END IF;
END
\$\$;
EOF
done

echo "▶ [5/5] Registering & restarting Debezium connector..."

# Kafka Connect 헬스체크
for i in {1..30}; do
  if curl -s ${CONNECT_URL}/ > /dev/null 2>&1; then
    break
  fi
  if [ $i -eq 30 ]; then
    echo "  ERROR: Kafka Connect not reachable"
    exit 1
  fi
  sleep 1
done

# connector가 없으면 등록, 있으면 삭제 후 재등록
if curl -s ${CONNECT_URL}/connectors | grep -q "${CONNECTOR_NAME}"; then
  echo "  Existing connector found, deleting..."
  curl -s -X DELETE ${CONNECT_URL}/connectors/${CONNECTOR_NAME}
  sleep 3
fi

echo "  Registering connector..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST ${CONNECT_URL}/connectors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "'"${CONNECTOR_NAME}"'",
    "config": {
      "connector.class": "io.debezium.connector.postgresql.PostgresConnector",

      "database.hostname": "postgres",
      "database.port": "5432",
      "database.user": "debezium",
      "database.password": "debezium",
      "database.dbname": "hirelog",

      "topic.prefix": "hirelog",

      "plugin.name": "pgoutput",
      "slot.name": "hirelog_outbox_slot",

      "publication.autocreate.mode": "disabled",
      "publication.name": "dbz_publication",

      "table.include.list": "public.outbox_event",

      "snapshot.mode": "never",
      "tombstones.on.delete": "false",
      "decimal.handling.mode": "string",

      "transforms": "outbox",
      "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",

      "transforms.outbox.table.field.event.id": "id",
      "transforms.outbox.table.field.event.key": "aggregate_id",
      "transforms.outbox.table.field.event.type": "event_type",
      "transforms.outbox.table.field.event.payload": "payload",

      "transforms.outbox.route.by.field": "aggregate_type",
      "transforms.outbox.route.topic.replacement": "hirelog.outbox.${routedByValue}",

      "key.converter": "org.apache.kafka.connect.storage.StringConverter",
      "value.converter": "org.apache.kafka.connect.json.JsonConverter",
      "value.converter.schemas.enable": "false"
    }
  }')

if [ "$HTTP_CODE" != "201" ] && [ "$HTTP_CODE" != "200" ]; then
  echo "  ERROR: Connector registration failed (HTTP ${HTTP_CODE})"
  exit 1
fi

echo "  Connector registered, waiting for WAL sync..."
sleep 5

# connector restart로 WAL 위치 재인식
curl -s -X POST ${CONNECT_URL}/connectors/${CONNECTOR_NAME}/restart > /dev/null 2>&1
sleep 3

# 상태 확인
STATUS=$(curl -s ${CONNECT_URL}/connectors/${CONNECTOR_NAME}/status)
CONNECTOR_STATE=$(echo "$STATUS" | grep -o '"state":"[^"]*"' | head -1 | cut -d'"' -f4)
TASK_STATE=$(echo "$STATUS" | grep -o '"state":"[^"]*"' | tail -1 | cut -d'"' -f4)

echo ""
echo "============================================"
echo "  CDC Setup Complete"
echo ""
echo "  wal_level    : logical"
echo "  debezium user: ${DEBEZIUM_USER}"
echo "  publication  : dbz_publication"
echo "  tables       : ${OUTBOX_TABLES[*]}"
echo "  connector    : ${CONNECTOR_STATE}"
echo "  task         : ${TASK_STATE}"
if [ "$CONNECTOR_STATE" = "RUNNING" ] && [ "$TASK_STATE" = "RUNNING" ]; then
  echo ""
  echo "  ✅ Ready"
else
  echo ""
  echo "  ⚠️ Connector may not be healthy, check:"
  echo "  curl ${CONNECT_URL}/connectors/${CONNECTOR_NAME}/status"
fi
echo "============================================"
