#!/usr/bin/env bash
# Creates the topic the Avro producer and consumer use, in the Kafka started by compose.yaml.
# Usage: scripts/create-topic.sh [topic]   (default: messages)
set -euo pipefail

topic="${1:-messages}"
module_dir="$(cd "$(dirname "$0")/.." && pwd)"

docker compose --project-directory "$module_dir" exec kafka /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server kafka:19092 \
    --create --if-not-exists \
    --topic "$topic" \
    --partitions 1 \
    --replication-factor 1

docker compose --project-directory "$module_dir" exec kafka /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server kafka:19092 \
    --describe \
    --topic "$topic"
