# Game Service — Spring Boot migration

Migration of `anticheat-backend-game-service` (plain Java microservice, hand-rolled gRPC +
unpooled JDBC) to Spring Boot, following the same pattern already applied to
`anticheat-backend-user-service`.

## Current stack

- Spring Boot 3.5.4 (`spring-boot-starter-parent`)
- `net.devh:grpc-server-spring-boot-starter` — serves `GameService` (gRPC, port 9091 by
  default), the same contract consumed by
  `anticheat-backend-gateway/grpc/ServiceClients.java`
  (`GameServiceGrpc.GameServiceBlockingStub` against `game-service:9091`). The stubs come
  from the shared module `com.chessfraud:grpc-api` (`anticheat-backend-libs/grpc-api`).
- `spring-boot-starter-web` + `spring-boot-starter-actuator` — **only** for
  `/actuator/health*` on port 9092 (equivalent to the old `utils/HealthServer.java`);
  there is no business `@RestController`
- `spring-boot-starter-data-jdbc` — replaces the hand-rolled `utils/Database.java`
  (unpooled `DriverManager.getConnection()` per call) with a HikariCP-backed `DataSource`
- PostgreSQL (`org.postgresql:postgresql`), the same instance shared with `user-service`
  and `analysis-service` (`anticheat-backend-infra/docker/init.sql`)
- Packaged with `spring-boot-maven-plugin` (not `maven-shade-plugin`)

## Package structure

Root package `com.chessfraud.gameservice`:

```
com.chessfraud.gameservice/
  GameServiceApplication.java        @SpringBootApplication
  model/
    game/
      Game.java                     Spring Data JDBC entity, @Table("games"), auto-generated id
  repository/
    game/
      GameRepository.java           interface extends CrudRepository<Game, Integer>
  service/
    game/
      GamePersistenceService.java   @Service, @Transactional
  dto/
    game/
      GamesResult.java / SaveGameResult.java   internal service results
  grpc/
    GameGrpcService.java            extends GameServiceGrpc.GameServiceImplBase, @GrpcService
```

## What's done

- **Spring Boot bootstrap**: `GameServiceApplication` with `@SpringBootApplication` +
  `SpringApplication.run(...)`.
- **gRPC instead of a hand-rolled `io.grpc.ServerBuilder`**: `GameGrpcService` implements
  `GameServiceGrpc.GameServiceImplBase` with the two RPCs from the shared `.proto`
  (`GetGames`, `SaveGame`). Same default host/port (`game-service:9091`) already expected
  by the Gateway — no need to touch `ServiceClients.java`.
- **`Game` as a Spring Data JDBC entity with an auto-generated id**: unlike `User` in
  user-service (natural key, `Persistable<String>`), `games.id` is a Postgres `SERIAL`, so
  the default "id is null → insert, id is set → update" behaviour Spring Data JDBC assumes
  works out of the box — no `Persistable` needed.
- **Cross-table update kept transactional, without duplicating the `User` entity**:
  `saveGame` both inserts into `games` (via `GameRepository`, owned by this service) and
  updates the caller's aggregate counters in `users` (`total_games`/`fair_games`/
  `cheated_games`) — a table conceptually owned by user-service but physically shared in
  the same Postgres instance. Rather than pull in a full `User` entity/repository here
  just to run one `UPDATE ... SET x = x + 1`, `GamePersistenceService` uses a `JdbcTemplate`
  for that statement, wrapped in the same `@Transactional` method as the `GameRepository`
  insert — both run on the same connection/transaction, same atomicity as the original
  `connection.setAutoCommit(false)` + `commit()`. On any `DataAccessException`,
  `TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()` is set before
  returning a failed `SaveGameResult`, replicating the original's implicit rollback-on-close.
- **No existence check before the counter update** (kept identical to the original): if
  `SaveGame` is called for a username that doesn't exist in `users`, the game row is still
  inserted and the `UPDATE` simply matches 0 rows — `SaveGameResult` still reports success.
  Covered by `saveGameForUnknownUserStillSucceeds`.
- **Health check with Actuator**: dedicated port 9092, separate from gRPC (9091) — same
  port layout as the original (`docker-compose.yml`: `9092:9091`, `9093:9092`).
  `management.endpoint.health.probes.enabled=true` exposes
  `/actuator/health/liveness` and `/actuator/health/readiness` separately; the *readiness*
  group includes the state of the database connection.
- **Real automated tests**: `GameGrpcServiceTest` boots the full Spring context with an
  in-process gRPC server (no network, no Postgres — in-memory H2) and exercises
  save→get round-trip, counter updates for both legal/cheated outcomes, the "unknown user"
  edge case, and an empty result for a user with no games. `mvn test` runs all four.

## What was deliberately left out

- **No input validation** (`user`/`moves` blank checks): the original
  `GameServiceGrpcImpl` had none either, so this migration doesn't add scope creep here —
  unlike `user-service`'s `UserGrpcService`, which already had `INVALID_ARGUMENT` checks in
  the code being migrated.
- **The in-memory result cache pattern used in `analysis-service` doesn't apply here** —
  `GetGames`/`SaveGame` are not idempotent-safe to cache (game lists change on every save).

## How to run

Requires a reachable PostgreSQL instance (use `docker-compose up postgres` from
`anticheat-backend/`, or set `DB_HOST`/`DB_USER`/`DB_PASSWORD` to point at your own). Also
requires `anticheat-backend-libs` installed in the local Maven repository
(`cd ../anticheat-backend-libs && mvn install -DskipTests`) before building this module.

```bash
mvn package
DB_HOST=localhost DB_USER=postgres DB_PASSWORD=postgres java -jar target/game-service-1.0-SNAPSHOT.jar
```

The service listens for gRPC on port 9091 and Actuator (HTTP) on 9092. To test the
business contract (save/get) use `mvn test` (in-process, no external dependencies) or a
gRPC client like `grpcurl` against port 9091.

## Environment variables

| Variable | Description | Default |
|----------|-------------|-------------|
| `GRPC_PORT` | gRPC server port | `9091` |
| `HEALTH_PORT` | Actuator port (`/actuator/health*`) | `9092` |
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `anticheat` |
| `DB_USER` | Database user | `postgres` |
| `DB_PASSWORD` | Database password | *(empty)* |
