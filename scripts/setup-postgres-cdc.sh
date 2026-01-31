#!/usr/bin/env bash
set -e

POSTGRES_CONTAINER=pg_hirelog
POSTGRES_USER=postgres
POSTGRES_DB=hirelog
DEBEZIUM_USER=debezium
DEBEZIUM_PASSWORD=debezium

echo "▶ Entering Postgres container..."
docker exec -i ${POSTGRES_CONTAINER} bash <<EOF
set -e

CONF_FILE=/var/lib/postgresql/data/postgresql.conf
HBA_FILE=/var/lib/postgresql/data/pg_hba.conf

echo "▶ Updating postgresql.conf..."
sed -i "s/^#*wal_level.*/wal_level = logical/" \$CONF_FILE
sed -i "s/^#*max_replication_slots.*/max_replication_slots = 10/" \$CONF_FILE
sed -i "s/^#*max_wal_senders.*/max_wal_senders = 10/" \$CONF_FILE

echo "▶ Updating pg_hba.conf..."
echo "host replication ${DEBEZIUM_USER} 0.0.0.0/0 md5" >> \$HBA_FILE
echo "host ${POSTGRES_DB} ${DEBEZIUM_USER} 0.0.0.0/0 md5" >> \$HBA_FILE
EOF

echo "▶ Restarting Postgres..."
docker restart pg_hirelog

echo "▶ Waiting for Postgres to be ready..."
for i in {1..30}; do
  if docker exec pg_hirelog pg_isready -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" > /dev/null 2>&1; then
    echo "✅ Postgres is ready"
    break
  fi
  sleep 1
done

echo "▶ Creating Debezium user & grants..."
docker exec -i ${POSTGRES_CONTAINER} \
  psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} <<EOF

-- Debezium 유저 생성 (idempotent)
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '${DEBEZIUM_USER}') THEN
    CREATE ROLE ${DEBEZIUM_USER}
      WITH LOGIN REPLICATION PASSWORD '${DEBEZIUM_PASSWORD}';
  END IF;
END
\$\$;

-- DB / 스키마 / 테이블 권한
GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO ${DEBEZIUM_USER};
GRANT CREATE ON DATABASE ${POSTGRES_DB} TO ${DEBEZIUM_USER};
GRANT USAGE ON SCHEMA public TO ${DEBEZIUM_USER};
GRANT SELECT ON TABLE public.outbox_event TO ${DEBEZIUM_USER};

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT ON TABLES TO ${DEBEZIUM_USER};

-- 🔴 publication은 postgres가 직접 생성
DO \$\$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_publication WHERE pubname = 'dbz_publication'
  ) THEN
    CREATE PUBLICATION dbz_publication
    FOR TABLE public.outbox_event;
  END IF;
END
\$\$;

EOF

echo "✅ Postgres CDC setup completed."

echo "▶ Restarting Debezium connector (if exists)..."
CONNECT_URL="http://localhost:8083"
CONNECTOR_NAME="hirelog-outbox-connector"

if curl -s ${CONNECT_URL} > /dev/null; then
  if curl -s ${CONNECT_URL}/connectors | grep -q "${CONNECTOR_NAME}"; then
    curl -s -X POST ${CONNECT_URL}/connectors/${CONNECTOR_NAME}/restart
    echo "✅ Connector restart requested"
  else
    echo "ℹ️ Connector not found, skip restart"
  fi
else
  echo "⚠️ Kafka Connect not reachable"
fi
