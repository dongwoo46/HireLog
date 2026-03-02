#!/usr/bin/env bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
ENV_FILE="${SCRIPT_DIR}/../.env.prod"

if [ ! -f "$ENV_FILE" ]; then
  echo "❌ .env.prod not found at $ENV_FILE"
  exit 1
fi

source "$ENV_FILE"

TOPICS=(
  "jd.preprocess.response"
  "jd.preprocess.response.fail"
  "hirelog.outbox.JobSummary"
  "hirelog.outbox.JdPreprocessText"
  "hirelog.outbox.JdPreprocessOcr"
  "hirelog.outbox.JdPreprocessUrl"
)

echo "=========================================="
echo "Creating Kafka Topics..."
echo "=========================================="
echo ""

for topic in "${TOPICS[@]}"
do
  echo "Creating topic: $topic"

  docker exec kafka_prod sh -c "
    unset KAFKA_OPTS && \
    /opt/kafka/bin/kafka-topics.sh \
      --bootstrap-server localhost:9092 \
      --create \
      --topic \"$topic\" \
      --partitions 3 \
      --replication-factor 1 \
      --if-not-exists
  "

  echo ""
done

echo "=========================================="
echo "✅ All topics created!"
echo "=========================================="
echo ""

echo "=========================================="
echo "Listing all topics:"
echo "=========================================="

docker exec kafka_prod sh -c "
  unset KAFKA_OPTS && \
  /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server localhost:9092 \
    --list
"

echo ""
echo "=========================================="
echo "Topic Details:"
echo "=========================================="

for topic in "${TOPICS[@]}"
do
  echo ""
  echo "--- Topic: $topic ---"

  docker exec kafka_prod sh -c "
    unset KAFKA_OPTS && \
    /opt/kafka/bin/kafka-topics.sh \
      --bootstrap-server localhost:9092 \
      --describe \
      --topic \"$topic\"
  "
done

echo ""
echo "=========================================="
echo "🎉 All done!"
echo "=========================================="
