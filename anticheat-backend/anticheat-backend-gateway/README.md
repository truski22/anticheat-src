# Gateway

WebSocket-to-MQTT bridge that connects browser clients to the backend microservices.

## Overview

The gateway exposes a WebSocket endpoint and translates client messages into typed MQTT messages, routing them to the appropriate backend service. Responses from services are forwarded back to the originating WebSocket session.

## Communication

| Direction | Protocol | Details |
|-----------|----------|---------|
| Inbound   | WebSocket | `ws://host:8080/ws/{topic}` |
| Outbound  | MQTT | Publishes to topic-specific channels on `tcp://mosquitto:1883` |

## Supported Message Types

| Message | Description |
|---------|-------------|
| `LoginRequest` | User authentication |
| `RegisterRequest` | New user registration |
| `ChangePassword` / `ChangePasswordSendEmail` | Password management |
| `UserInfoRequest` | Fetch user profile |
| `GamesRequest` | Retrieve saved games |
| `AnalyzeGame` | Submit a game for ML analysis |
| `SaveGame` | Persist a game record |

## Requirements

- Java 11+
- MQTT broker running at `tcp://mosquitto:1883`
- Maven 3+

## Dependencies

This service depends on local libraries that must be installed first:

- `shared` — common DTOs and utilities
- `ws-lib` — WebSocket server abstraction
- `mqtt-lib` — MQTT client abstraction

## How to Run

```bash
# Install local dependencies
mvn -f shared/pom.xml install
mvn -f ws-lib/pom.xml install
mvn -f mqtt-lib/pom.xml install

# Build and run
mvn -f gateway/pom.xml package
java -jar gateway/target/gateway-1.0-SNAPSHOT.jar
```

The gateway listens on **port 8080**.
