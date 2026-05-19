#!/bin/bash

# Path to your Kafka install — override with KAFKA_HOME env var if needed
KAFKA="${KAFKA_HOME:-$HOME/Downloads/kafka_2.13-4.2.0}"

if [ ! -d "$KAFKA" ]; then
  echo "[kafka] ERROR: Kafka not found at $KAFKA"
  echo "[kafka] Set KAFKA_HOME to your Kafka directory, e.g.:"
  echo "[kafka]   export KAFKA_HOME=~/Downloads/kafka_2.13-4.2.0"
  exit 1
fi

echo "[kafka] Using Kafka at: $KAFKA"

CONFIG="$KAFKA/config/server.properties"
LOG_DIR=$(grep "^log.dirs" "$CONFIG" | cut -d'=' -f2 | tr -d ' ')
LOG_DIR="${LOG_DIR:-/tmp/kraft-combined-logs}"

# Format storage if not already formatted (KRaft requires this before first start)
if [ ! -f "$LOG_DIR/meta.properties" ]; then
  echo "[kafka] Formatting storage for KRaft (first-time setup)..."
  UUID=$("$KAFKA/bin/kafka-storage.sh" random-uuid)
  "$KAFKA/bin/kafka-storage.sh" format \
    --standalone \
    -t "$UUID" \
    -c "$CONFIG"
  echo "[kafka] Storage formatted with UUID: $UUID"
else
  echo "[kafka] Storage already formatted, skipping."
fi

# Start broker
echo "[kafka] Starting broker..."
"$KAFKA/bin/kafka-server-start.sh" "$CONFIG" &
KAFKA_PID=$!

# Wait until broker is accepting connections
echo "[kafka] Waiting for broker to be ready..."
for i in $(seq 1 20); do
  "$KAFKA/bin/kafka-topics.sh" --list --bootstrap-server localhost:9092 > /dev/null 2>&1 && break
  sleep 2
  echo "[kafka] Still waiting... ($i/20)"
done

# Create topics (safe to re-run — skips if already exist)
echo "[kafka] Creating topics..."
for TOPIC in shield.requests shield.decisions shield.anomalies; do
  "$KAFKA/bin/kafka-topics.sh" --create --if-not-exists \
    --topic "$TOPIC" \
    --partitions 1 \
    --replication-factor 1 \
    --bootstrap-server localhost:9092 \
    && echo "[kafka] ✓ $TOPIC"
done

echo "[kafka] All topics ready."
wait $KAFKA_PID