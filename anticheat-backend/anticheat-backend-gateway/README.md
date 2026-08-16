# Gateway

Spring Boot service that exposes the public WebSocket and REST auth endpoints for browser
clients and translates them into gRPC calls to the internal backend services.

## Overview

The gateway is the only backend service exposed outside the cluster (see
`anticheat-backend-infra/k8s/06-gateway.yaml`). It terminates client WebSocket connections,
authenticates them via JWT, and routes each typed message to `user-service`, `game-service`,
or `analysis-service` over gRPC. Password-related flows (login, register, forgot/reset
password, authenticated change-password) are plain REST endpoints instead, since they don't
require an open WebSocket connection.

## Communication

| Direction | Protocol | Details |
|-----------|----------|---------|
| Inbound   | WebSocket | `ws://host:8080/ws?token=<JWT>` |
| Inbound   | REST | `POST http://host:8080/auth/{login,register,forgot-password,reset-password,change-password}` |
| Outbound  | gRPC | `user-service:9090`, `game-service:9091`, `analysis-service:9092` (see `gateway.services.*` in `application.properties`) |

## Supported WebSocket Message Types

| Message | Description |
|---------|-------------|
| `USER_INFO_REQUEST` | Fetch user profile |
| `GAMES_REQUEST` | Retrieve saved games |
| `CHANGE_PASSWORD` | Password change for the authenticated user |
| `ANALYZE_GAME` | Submit a game for ML analysis |
| `SAVE_GAME` | Persist a game record |

## Requirements

- Java 21+
- Maven 3+
- `JWT_SECRET` environment variable (fails fast at startup if unset)

## Dependencies

This service depends on local libraries that must be installed first (see
`anticheat-backend-libs/README.md`):

- `protocol` - WebSocket wire protocol (message envelope, typed payloads)
- `grpc-api` - generated gRPC client stubs for the internal services

## How to Run

```bash
# Install local dependencies
mvn -f anticheat-backend-libs/pom.xml install

# Build and run
mvn -f anticheat-backend-gateway/pom.xml package
JWT_SECRET=$(openssl rand -hex 32) java -jar anticheat-backend-gateway/target/gateway-1.0-SNAPSHOT.jar
```

The gateway listens on **port 8080** (WebSocket + REST) and **port 8081** (Actuator health).
