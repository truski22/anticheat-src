# anticheat-backend-libs - architecture

## Module boundaries

| Module | Status | Depends on a framework? | Consumed by |
|---|---|---|---|
| [`protocol`](protocol/README.md) | Active, canonical | No (Jackson + SLF4J API only) | `gateway` |
| [`grpc-api`](grpc-api) | Active, canonical | No (protobuf/gRPC only) | `gateway`, `user-service`, `game-service`, `analysis-service` |
| [`ws-lib`](ws-lib/README.md) | **Legacy - being retired** | Tyrus/`javax.websocket` | `gateway` |

Every module here is a plain library JAR (`com.chessfraud:<artifactId>:1.0-SNAPSHOT`),
never a runnable service. Nothing in this reactor pulls in Spring - that's deliberate,
see below.

## Design rule: this reactor stays framework-agnostic

`anticheat-backend-libs` is shared by every backend service, and those services are on
different runtimes today (`user-service` is Spring Boot; `gateway`, `game-service`, and
`analysis-service` are plain Java with hand-rolled `main()` entry points) and are all
converging on **Spring Boot**. A shared library that pulls in a specific web framework
becomes a migration obstacle, not an accelerant - so `protocol` and `grpc-api` depend on
nothing framework-specific: plain records, Jackson, protobuf-generated stubs, SLF4J's
API (never a concrete logging backend - see each module's README). Both drop into a
Spring `@RestController`, a Spring `TextWebSocketHandler`, or a
`net.devh:grpc-server-spring-boot-starter` service with zero changes.

`ws-lib` is the exception, and it's exempted on purpose: it exists solely to keep the
still-not-migrated `gateway` module's current Tyrus-based WebSocket endpoint running.
It is explicitly out of scope for further investment - see its README for the retirement
plan.

## What used to be here and was removed

Earlier versions of the `protocol` module shipped a `utils` package (`Database`,
`HealthServer`, `JsonUtils`) alongside the message protocol. It was removed during the
2026-08 cleanup because:

1. **No consumer actually depended on it.** `game-service` and `analysis-service` each
   carry their own local, independently-drifted forks of `Database`/`HealthServer`/
   `JsonUtils` under their own `utils` package instead of depending on this module - the
   shared version was dead weight in every build that included it.
2. **It's superseded by Spring Boot starters**, which `user-service` already
   demonstrates: `spring-boot-starter-data-jdbc` auto-configures a HikariCP-backed
   `DataSource` from `spring.datasource.*` (no hand-rolled connection pool needed), and
   `spring-boot-starter-actuator` provides a production-grade `/actuator/health`
   endpoint (no hand-rolled `com.sun.net.httpserver.HttpServer` needed). As the
   remaining services migrate to Spring Boot, they should adopt those starters directly
   rather than a custom library wrapper.
3. Mixing DB pooling and an HTTP health endpoint into an artifact called `protocol` was
   itself a structure smell - unrelated concerns bundled for no reason other than "there
   was nowhere else to put them."

**Follow-up worth doing (not done as part of this cleanup, since it touches other
modules' source trees):** `game-service`'s local `Database` fork uses unpooled
`DriverManager.getConnection()` per call, which is strictly worse than the HikariCP
pooling the old shared version had. Once either service is on Spring Boot, switching to
`spring-boot-starter-data-jdbc` fixes this as a side effect; until then, replacing the
hand-rolled pool with a properly configured `HikariDataSource` locally would be a
reasonable interim fix.

## Versioning

All three modules share one version (`1.0-SNAPSHOT`) via the multi-module reactor and
are released together. Shared dependency versions (Jackson, SLF4J, Logback, JUnit) are
pinned once in the root `pom.xml`'s `<dependencyManagement>` and referenced by
`groupId`/`artifactId` only from child modules - update a version in exactly one place.
