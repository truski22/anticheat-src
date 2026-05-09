# Analysis Service

ML analysis orchestrator that bridges MQTT messaging with the Python ML service.

## Overview

Subscribes to the `game/analyze` MQTT topic, receives game data, forwards it to the ML service via HTTP for evaluation, and publishes the prediction results back over MQTT.

## MQTT Interface

| Topic | Direction | Message |
|-------|-----------|---------|
| `game/analyze` | Subscribe | `AnalyzeGame` |
| `game/analyze` | Publish | `AnalyzeGameResponse` |

## Flow

```
Client → Gateway → MQTT(game/analyze) → Analysis Service → HTTP POST → ml-service
                                              ↓
Client ← Gateway ← MQTT(AnalyzeGameResponse) ←
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `CHESS_INSIGHTS_URL` | `http://localhost:5002/eval` | ML service evaluation endpoint |

## Requirements

- Java 11+, Maven 3+
- MQTT broker
- `ml-service` running and reachable

## How to Run

```bash
mvn -f shared/pom.xml install
mvn -f mqtt-lib/pom.xml install
mvn -f analysis-service/pom.xml package
java -jar analysis-service/target/analysis-service-1.0-SNAPSHOT.jar
```

No exposed ports — communicates only via MQTT.
