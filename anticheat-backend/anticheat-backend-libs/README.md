# anticheat-backend-libs

Shared Maven reactor for libraries consumed by the anticheat backend services
(`gateway`, `user-service`, `game-service`, `analysis-service`).

| Module | Purpose |
|---|---|
| [`protocol`](protocol/README.md) | Client/gateway WebSocket wire protocol - message envelope, typed payloads, hardened (de)serializer |
| [`grpc-api`](grpc-api) | Generated gRPC client/server stubs for the internal `AnalysisService`, `GameService`, `UserService` contracts |

See [ARCHITECTURE.md](ARCHITECTURE.md) for module boundaries and the framework-agnostic
design rule this reactor follows.

## Build

```bash
mvn -f anticheat-backend-libs/pom.xml install
```

Each service depends on these as regular Maven coordinates, e.g.:

```xml
<dependency>
    <groupId>com.chessfraud</groupId>
    <artifactId>protocol</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```
