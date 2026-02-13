#!/bin/sh
set -e

echo "========================================"
echo "PgBouncer Initialization Starting..."
echo "========================================"

# PostgreSQL 연결 대기
echo "Waiting for PostgreSQL to be ready..."
MAX_RETRIES=30
RETRY_COUNT=0

until PGPASSWORD="${POSTGRES_PASSWORD}" psql -h postgres -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -c '\q' 2>/dev/null; do
  RETRY_COUNT=$((RETRY_COUNT + 1))
  if [ $RETRY_COUNT -ge $MAX_RETRIES ]; then
    echo "ERROR: PostgreSQL did not become ready in time"
    exit 1
  fi
  echo "PostgreSQL is unavailable - retry $RETRY_COUNT/$MAX_RETRIES..."
  sleep 2
done

echo "✅ PostgreSQL is ready!"

# userlist.txt 생성
echo "Creating /etc/pgbouncer/userlist.txt..."

PGPASSWORD="${POSTGRES_PASSWORD}" psql \
  -h postgres \
  -U "${POSTGRES_USER}" \
  -d "${POSTGRES_DB}" \
  -t -A \
  -c "SELECT '\"' || usename || '\" \"' || passwd || '\"' FROM pg_shadow WHERE usename = '${POSTGRES_USER}'" \
  > /etc/pgbouncer/userlist.txt

# 파일이 비어있으면 fallback
if [ ! -s /etc/pgbouncer/userlist.txt ]; then
  echo "WARNING: userlist.txt is empty! Using plaintext fallback..."
  echo "\"${POSTGRES_USER}\" \"${POSTGRES_PASSWORD}\"" > /etc/pgbouncer/userlist.txt
fi

# ✅ 파일 권한 설정 (nobody가 읽을 수 있도록)
chown nobody:nobody /etc/pgbouncer/userlist.txt
chmod 600 /etc/pgbouncer/userlist.txt

echo "✅ userlist.txt created successfully:"
echo "========================================"
cat /etc/pgbouncer/userlist.txt
echo "========================================"

# PgBouncer 설정 검증
if [ ! -f /etc/pgbouncer/pgbouncer.ini ]; then
  echo "ERROR: pgbouncer.ini not found!"
  exit 1
fi

echo "✅ Configuration files ready"
echo "Starting PgBouncer..."
echo "========================================"

# PgBouncer 실행 (nobody 유저로)
exec su-exec nobody /usr/bin/pgbouncer /etc/pgbouncer/pgbouncer.ini