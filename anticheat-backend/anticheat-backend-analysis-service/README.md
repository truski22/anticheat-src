# Analysis Service — Spring Boot migration

Migration of `anticheat-backend-analysis-service` (plain Java microservice, hand-rolled
gRPC + `java.net.http.HttpClient`) to Spring Boot, following the same pattern already
applied to `anticheat-backend-user-service` and `anticheat-backend-game-service`.

## Current stack

- Spring Boot 3.5.4 (`spring-boot-starter-parent`)
- `net.devh:grpc-server-spring-boot-starter` — serves `AnalysisService` (gRPC, port 9092
  by default), the same contract consumed by
  `anticheat-backend-gateway/grpc/ServiceClients.java`
  (`AnalysisServiceGrpc.AnalysisServiceBlockingStub` against `analysis-service:9092`). The
  stubs come from the shared module `com.chessfraud:grpc-api`
  (`anticheat-backend-libs/grpc-api`).
- `spring-boot-starter-web` — provides both Actuator's HTTP endpoint and Spring's
  `RestClient`/Jackson, replacing the original hand-rolled `HttpClient` + manual JSON
  string building/parsing.
- `spring-boot-starter-actuator` — `/actuator/health*` on port 9093 (equivalent to the old
  `utils/HealthServer.java`); there is no business `@RestController`.
- No database — this service is a stateless HTTP orchestrator with an in-memory cache.
- Packaged with `spring-boot-maven-plugin` (not `maven-shade-plugin`)

## Package structure

Root package `com.chessfraud.analysisservice`:

```
com.chessfraud.analysisservice/
  AnalysisServiceApplication.java     @SpringBootApplication, @EnableConfigurationProperties
  config/
    AnalysisServiceConfig.java        @ConfigurationProperties(prefix = "analysis-service")
    MlServiceClientConfig.java        @Bean RestClient (bearer token, 5s/10s timeouts)
  dto/
    analysis/
      AnalysisResult.java             internal service result (unchanged shape)
    ml/
      MlPredictRequest.java           record: outbound JSON body to ml-service
      MlPredictResponse.java          record: inbound JSON body from ml-service
  service/
    analysis/
      GameAnalysisService.java        @Service, SHA-256 result cache, calls RestClient
  grpc/
    AnalysisGrpcService.java          extends AnalysisServiceGrpc.AnalysisServiceImplBase, @GrpcService
```

## What's done

- **Spring Boot bootstrap**: `AnalysisServiceApplication` with `@SpringBootApplication` +
  `SpringApplication.run(...)`.
- **gRPC instead of a hand-rolled `io.grpc.ServerBuilder`**: `AnalysisGrpcService`
  implements `AnalysisServiceGrpc.AnalysisServiceImplBase` with the single RPC from the
  shared `.proto` (`AnalyzeGame`). Same default host/port (`analysis-service:9092`)
  already expected by the Gateway — no need to touch `ServiceClients.java`. Behaviour
  preserved exactly: blank `user`/`moves` → `INVALID_ARGUMENT`, any failure calling
  ml-service → `INTERNAL`.
- **`RestClient` replaces the hand-rolled `HttpClient`**, and with it two real bugs fixed,
  not just moved:
  - The original built the outbound JSON body with
    `String.format("{ \"moves\": \"%s\" }", moves.replace("\"", "\\\""))`, which only
    escapes double quotes — a move string containing a backslash or control character
    produced invalid JSON. `MlPredictRequest` is now a plain record serialized by Jackson
    via `RestClient`, which escapes correctly.
  - The original parsed the response with a bespoke `JsonUtils` (`extractNestedField`,
    `extractIntList`) duplicated across this and the now-removed `utils/JsonUtils.java`.
    `MlPredictResponse` is now a typed record bound directly by Jackson.
  - `MlServiceHttpClient` — a dead, unused duplicate of the same HTTP-calling logic that
    already lived in `GameAnalysisService` — was deleted rather than migrated.
- **Cache behaviour unchanged**: `GameAnalysisService` still keeps the SHA-256-keyed,
  size-bounded (`LinkedHashMap` access-order LRU, 500 entries) in-memory cache, guarded by
  the same `synchronized` blocks. This only works correctly with `replicas: 1`
  (`anticheat-backend-infra/k8s/09-analysis-service.yaml`), same limitation as the
  original — not addressed by this migration.
- **Fail-fast on missing token, same message**: the original constructor threw
  `IllegalStateException("[ANALYSIS] FATAL: ML_SERVICE_TOKEN environment variable is not
  set.")`. `AnalysisServiceConfig` now does the same check in a `@PostConstruct` method,
  so a misconfigured deployment still refuses to start rather than silently sending
  unauthenticated requests.
- **Removed vestigial `src/main/resources/stockfish.exe`**: unreferenced anywhere in this
  module's code — Stockfish runs inside `ml-service`, not here.
- **Health check with Actuator**: dedicated port 9093, separate from gRPC (9092) — same
  port layout as the original (`docker-compose.yml`: `9094:9092`, `9095:9093`).
- **Real automated tests**: `AnalysisGrpcServiceTest` boots the full Spring context with an
  in-process gRPC server and a lightweight embedded `com.sun.net.httpserver.HttpServer`
  standing in for ml-service (bound via `@DynamicPropertySource`, no real network call, no
  dependency on ml-service/Stockfish being up). Covers a successful prediction (including
  the `Authorization: Bearer <token>` header actually sent), the cache only hitting
  ml-service once for repeated identical moves, and `INVALID_ARGUMENT` on blank fields.

## What was deliberately left out

- **No retry/circuit-breaker around the ml-service call** — same as the original; a
  transient ml-service failure still surfaces as `Status.INTERNAL` to the Gateway.
- **The cache is still process-local**, not shared across replicas (see above) — fixing it
  (e.g. moving to a shared cache) is out of scope for this migration.

## How to run

Requires `anticheat-backend-libs` installed in the local Maven repository
(`cd ../anticheat-backend-libs && mvn install -DskipTests`) before building this module,
and a reachable ml-service (`docker-compose up ml-service` from `anticheat-backend/`, or
point `CHESS_INSIGHTS_URL` elsewhere).

```bash
mvn package
CHESS_INSIGHTS_URL=http://localhost:5002/eval ML_SERVICE_TOKEN=change-me java -jar target/analysis-service-1.0-SNAPSHOT.jar
```

The service listens for gRPC on port 9092 and Actuator (HTTP) on 9093. To test the
business contract (`AnalyzeGame`) use `mvn test` (in-process, no external dependencies) or
a gRPC client like `grpcurl` against port 9092.

## Environment variables

| Variable | Description | Default |
|----------|-------------|-------------|
| `GRPC_PORT` | gRPC server port | `9092` |
| `HEALTH_PORT` | Actuator port (`/actuator/health*`) | `9093` |
| `CHESS_INSIGHTS_URL` | Full URL of the ml-service inference endpoint | `http://localhost:5002/predict` |
| `ML_SERVICE_TOKEN` | Bearer token authenticating against ml-service — **required**, startup fails without it | *(none)* |
