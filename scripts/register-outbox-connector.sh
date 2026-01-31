#!/usr/bin/env bash

curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "hirelog-outbox-connector",
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
  }'
