# Adaptive API Shield

An event-driven API protection and anomaly analysis platform built to explore production-grade distributed systems patterns — adaptive rate limiting, behavioral trust scoring, Kafka-based streaming pipelines, and AI-assisted incident investigation.

The system is intentionally split into two independent execution paths: a **deterministic hot path** for low-latency enforcement and an **asynchronous cold path** for behavioral analysis. AI inference never blocks request processing.

---

## Architecture

```
                        ┌─────────────────────────────────────────┐
                        │           HOT PATH (< 5ms)              │
  Client Request ──────▶│  Spring Boot Gateway                    │──────▶ Backend API
                        │  └─ Redis: rate limits + trust scores   │
                        │  └─ Allow / Throttle / Block            │
                        └──────────────┬──────────────────────────┘
                                       │ publish event (async, non-blocking)
                                       ▼
                        ┌─────────────────────────────────────────┐
                        │           COLD PATH (async)             │
                        │  Kafka: shield.requests                 │
                        │  └─ Anomaly Detection Consumer          │
                        │  Kafka: shield.anomalies                │
                        │  └─ FastAPI AI Analyzer                 │
                        │     └─ Groq LLM → Incident Explanation  │
                        └─────────────────────────────────────────┘
```

The enforcement layer is stateless and resilient — it degrades gracefully if Kafka or the AI analyzer go down. The cold path can be upgraded, redeployed, or scaled independently without any impact on live traffic.

---

## Key Design Decisions

**Why separate hot and cold paths?**
Putting AI inference in the request path would introduce unpredictable latency and create a single point of failure. The enforcement layer stays deterministic; the AI layer operates on a best-effort async basis. This pattern mirrors production API gateway architectures where SLA compliance and analysis correctness are separate concerns.

**Why Redis for enforcement state?**
Trust scores, rate limit counters, and blocklists need to be shared across horizontally scaled gateway nodes. Redis TTLs handle automatic expiration without cleanup jobs — a rate limit window resets itself, and a temporary block expires without any scheduled task.

**Why progressive cooldowns instead of permanent bans?**
Fixed cooldowns are predictable — attackers simply wait for expiration and retry. Progressive cooldowns increase attacker cost exponentially while avoiding permanent lockouts that could affect legitimate users recovering from credential issues.

**Why Kafka between enforcement and analysis?**
Decoupling via Kafka means the anomaly detection consumer and AI analyzer can fall behind, restart, or be replaced without any impact on the gateway. The request stream is also replayable — useful for reprocessing historical events with new detection logic.

---

## Core Features

### Adaptive Rate Limiting
Endpoint-aware distributed rate limiting backed by Redis. Login endpoints use strict thresholds; read-heavy endpoints allow higher throughput. Counters reset automatically via TTL.

### Trust Score System
Each client maintains a dynamic behavioral score in Redis. Successful requests increase trust; failed logins and throttle events degrade it. Clients that drop below the trust threshold are temporarily blocked with progressive cooldowns.

### Kafka Event Streaming
Every request publishes a structured event to `shield.requests` containing client ID, endpoint, status, trust score, and timestamp. A separate consumer detects anomalous patterns and publishes to `shield.anomalies`.

### AI-Assisted Incident Analysis
The FastAPI analyzer consumes anomaly events and sends structured prompts to Groq-hosted Llama 3.1. Generated incident reports include risk level, behavioral explanation, mitigation recommendations, and confidence estimate — all produced asynchronously without touching the request path.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Gateway & Enforcement | Java 17, Spring Boot, Gradle |
| Distributed State | Redis |
| Event Streaming | Apache Kafka |
| AI Analysis | Python, FastAPI, Groq API, Llama 3.1 |

---

## Project Structure

```
adaptive-api-shield/
├── shield-gateway/               # Spring Boot enforcement layer
│   ├── GatewayController         # Request handling + forwarding
│   ├── RateLimiterService        # Redis-backed distributed rate limiting
│   ├── TrustScoreService         # Behavioral trust scoring + blocking
│   ├── RedisConfig
│   └── kafka/
│       ├── config/               # Producer + consumer configuration
│       ├── consumer/             # Anomaly detection consumer
│       ├── producer/             # Request + anomaly event publishers
│       └── event/                # RequestEvent, AnomalyEvent models
├── demo-backend/                 # Downstream API (simulates protected service)
├── ai-analyzer/
│   └── main.py                   # FastAPI AI analysis service
└── README.md
```

---

## Running Locally

### Prerequisites
- Java 17
- Python 3.10+
- Redis
- Apache Kafka
- Gradle

### 1. Start Infrastructure

```bash
# Redis
redis-server

# Kafka (from Kafka install directory)
bin/kafka-server-start.sh config/server.properties

# Create topics
bin/kafka-topics.sh --create --topic shield.requests --bootstrap-server localhost:9092
bin/kafka-topics.sh --create --topic shield.anomalies --bootstrap-server localhost:9092
```

### 2. Start Spring Boot Services

```bash
./gradlew :demo-backend:bootRun
./gradlew :shield-gateway:bootRun
```

### 3. Start AI Analyzer

```bash
cd ai-analyzer
source .venv/bin/activate
uvicorn main:app --reload --port 8090
```

### Example Flow

1. Client sends repeated failed login attempts to the gateway
2. Gateway evaluates trust score and rate limits via Redis
3. Request event is published to `shield.requests` (non-blocking)
4. Anomaly consumer detects brute-force pattern
5. Anomaly event published to `shield.anomalies`
6. FastAPI analyzer generates an LLM-powered incident report

---

## What's Next

- **Observability**: Prometheus metrics + Grafana dashboards for request rate, trust score distribution, and anomaly rate
- **Containerization**: Docker Compose for one-command local setup
- **Cloud deployment**: AWS EC2 with managed Kafka (Confluent Cloud)
- **Resilience**: Circuit breaker on the async Kafka consumer
- **Rate limiting algorithms**: Sliding window and token bucket variants
- **Persistent storage**: Anomaly history and audit trail in PostgreSQL

---

## Author

Divita Phadakale — M.S. Computer Science, University of Colorado Boulder