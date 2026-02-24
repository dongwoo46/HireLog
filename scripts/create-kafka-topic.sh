#!/bin/bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
ENV_FILE="${SCRIPT_DIR}/.env.dev"

if [ ! -f "$ENV_FILE" ]; then
  echo "❌ .env.dev not found at $ENV_FILE"
  exit 1
fi

source "$ENV_FILE"

export MSYS_NO_PATHCONV=1

echo "Creating client.properties..."

docker exec kafka_dev sh -c "cat > /tmp/client.properties <<EOF
security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${KAFKA_USERNAME}\" password=\"${KAFKA_PASSWORD}\";
EOF"

echo "✅ client.properties created"
echo ""

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

  docker exec kafka_dev sh -c "
    unset KAFKA_OPTS && \
    /opt/kafka/bin/kafka-topics.sh \
      --bootstrap-server localhost:9092 \
      --command-config /tmp/client.properties \
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

docker exec kafka_dev sh -c "
  unset KAFKA_OPTS && \
  /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server localhost:9092 \
    --command-config /tmp/client.properties \
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

  docker exec kafka_dev sh -c "
    unset KAFKA_OPTS && \
    /opt/kafka/bin/kafka-topics.sh \
      --bootstrap-server localhost:9092 \
      --command-config /tmp/client.properties \
      --describe \
      --topic \"$topic\"
  "
done

echo ""
echo "=========================================="
echo "🎉 All done!"
echo "=========================================="
