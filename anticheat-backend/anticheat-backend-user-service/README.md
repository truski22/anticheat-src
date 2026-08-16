# User Service — Spring Boot migration (in progress)

Learning migration of `anticheat-backend-user-service` (plain Java microservice, hand-rolled gRPC +
JDBC) to Spring Boot, done by hand step by step. This document reflects the actual state of the
code, not a plan — update it as you go.

## Current stack

- Spring Boot 3.5.4 (`spring-boot-starter-parent`)
- `net.devh:grpc-server-spring-boot-starter` — serves `UserService` (gRPC, port 9090 by default),
  the same contract consumed by `anticheat-backend-gateway/grpc/ServiceClients.java`
  (`UserServiceGrpc.UserServiceBlockingStub` against `user-service:9090`). The stubs come from the
  shared module `com.chessfraud:grpc-api` (`anticheat-backend-libs/grpc-api`) — the same
  dependency the Gateway already uses, not a local copy of the `.proto`.
- `spring-boot-starter-web` + `spring-boot-starter-actuator` — **only** for `/actuator/health*`
  on port 9091 (equivalent to the old `utils/HealthServer.java`); there is no business
  `@RestController`
- `spring-boot-starter-data-jdbc` (user repository)
- `spring-boot-starter-mail` (`JavaMailSender`, replaces the manual `jakarta.mail` code)
- PostgreSQL (`org.postgresql:postgresql`), the same instance shared with `game-service` and
  `analysis-service` (`anticheat-backend-infra/docker/init.sql`) — **not** its own database
- `jbcrypt`
- Packaged with `spring-boot-maven-plugin` (not `maven-shade-plugin`)

## Package structure

Root package `com.chessfraud.userservice`, organized **by type and, within each type, by
feature**:

```
com.chessfraud.userservice/
  UserServiceApplication.java        @SpringBootApplication, @EnableConfigurationProperties
  config/
    UserServiceConfig.java           @ConfigurationProperties(prefix = "user-service"): smtpUser/smtpPassword
    MailConfig.java                  @Bean JavaMailSender (Gmail SMTP/STARTTLS), reads UserServiceConfig
  model/
    user/
      User.java                     Spring Data JDBC entity, Persistable<String>, @Table("users")
  repository/
    user/
      UserRepository.java           interface extends CrudRepository<User, String>
  service/
    user/
      UserAccountService.java       @Service, depends on UserRepository
    email/
      EmailNotificationService.java @Service, uses JavaMailSender + MimeMessageHelper
  dto/
    user/
      LoginResult.java / RegisterResult.java / UserInfoResult.java   internal service results
      LoginOutcome.java / RegisterOutcome.java                       business enums (no magic strings)
  grpc/
    UserGrpcService.java            extends UserServiceGrpc.UserServiceImplBase, @GrpcService
```

There is no `web/` package and no JSON request/response DTOs: the input/output contract is the
Protobuf messages from `com.chessfraud:grpc-api`.

## What's done

- **Spring Boot bootstrap**: `UserServiceApplication` with `@SpringBootApplication` +
  `SpringApplication.run(...)`.
- **gRPC instead of REST**: `UserGrpcService` implements `UserServiceGrpc.UserServiceImplBase` with
  the six RPCs from the shared `.proto` (`Login`, `Register`, `GetUserInfo`, `ChangePassword`,
  `ChangePasswordByEmail`, `SendPasswordResetEmail`). Same default host/port
  (`user-service:9090`) already expected by the Gateway — no need to touch `ServiceClients.java`.
  Empty fields are rejected with `Status.INVALID_ARGUMENT`; a user not found in
  `GetUserInfo` returns `Status.NOT_FOUND`.
- **No scattered magic strings**: `UserAccountService` and the internal `*Result` types work with
  the `RegisterOutcome`/`LoginOutcome` enums (`OK`, `USERNAME_TAKEN`, `EMAIL_TAKEN`,
  `INVALID_CREDENTIALS`). The mapping to the wire literals expected by the Gateway (`"OK"` / `"UNV"` /
  `"ENV"` / `"KO"`, fixed by the `.proto`) lives in a single place: `UserGrpcService.toWireMessage(...)`.
- **Shared PostgreSQL, no dedicated H2**: `spring.datasource.url` points to
  `jdbc:postgresql://${DB_HOST}:${DB_PORT:5432}/${DB_NAME:anticheat}`, with the same env vars
  (`DB_HOST` from the `app-config` ConfigMap, `DB_USER`/`DB_PASSWORD` from the `anticheat-secret`
  Secret) that k8s already injects. `spring.sql.init.mode=never`: the schema is created by
  `postgres-init-config`, not by this app; `schema.sql` is kept only as reference/local use.
- **`User` with a natural key**: the real table (`init.sql`) uses `name` as `PRIMARY KEY`, with no
  auto-incrementing `id`. The entity implements `Persistable<String>` (`@PersistenceCreator`
  constructor for rows read from the DB = not new; `new User()` = new) because Spring Data JDBC,
  with a client-assigned rather than auto-generated key, can't decide INSERT vs. UPDATE just by
  checking whether the id is null. Columns are explicitly mapped with `@Column("total_games")`
  etc. — Spring Data JDBC's default naming doesn't match Postgres's identifier folding.
- **Registration race condition actually closed**: the `existsByName`/`existsByEmail` checks are
  still there (they give a fast response in the normal case), but they're no longer the only
  defense. If two concurrent registrations both pass these checks before either calls `save()`,
  Postgres's `UNIQUE` constraint makes the second `save()` fail with
  `DataIntegrityViolationException`; `UserAccountService.register(...)` catches it and returns the
  same outcome (`USERNAME_TAKEN`/`EMAIL_TAKEN`) as if it had been caught by the initial check,
  instead of letting a generic 500/`UNKNOWN` leak out. Covered by a test
  (`UserAccountServiceTest`, with `UserRepository` mocked to force the race).
- **Access control on `ChangePassword` actually closed, from the Gateway**: this service has never
  known who's calling (the `.proto` carries no identity), so that check can't live here.
  `anticheat-backend-gateway/auth/AuthController.java` now exposes `POST /auth/change-password`,
  which requires `Authorization: Bearer <jwt>` and calls `ChangePassword` with the username taken
  from the validated JWT — never one sent by the client in the body. Before this, no route in the
  Gateway invoked `ChangePassword` at all.
- **Fixed package**: `EmailNotificationService` had `package main.java.com.chessfraud...` (didn't
  match its actual folder `service/email/`) — fixed to
  `com.chessfraud.userservice.service.email`, matching its import in `MailConfig`.
- **Externalized configuration**: `UserServiceConfig` with `@ConfigurationProperties(prefix =
  "user-service")`. The `SMTP_USER`/`SMTP_PASSWORD` env vars are explicitly mapped in
  `application.properties`.
- **Dependency injection**: all beans wired through the constructor.
- **Health check with Actuator**: dedicated port 9091, separate from gRPC (9090) — the same port
  layout as the original design (`docker-compose.yml`: `9090:9090`, `9091:9091`).
  `management.endpoint.health.probes.enabled=true` exposes `/actuator/health/liveness` and
  `/actuator/health/readiness` separately; the *readiness* group includes the state of the
  database connection.
- **Real automated tests**: `UserGrpcServiceTest` boots the full Spring context with an in-process
  gRPC server (no network, no Postgres — in-memory H2) and exercises the contract exactly as the
  Gateway sees it: register + login, wrong credentials → `"KO"`, duplicate username/email →
  `"UNV"`/`"ENV"`, empty fields → `INVALID_ARGUMENT`, nonexistent user →
  `NOT_FOUND`. `UserAccountServiceTest` covers the registration race condition with Mockito.
  `mvn test` runs both (9/9 passing).

## What was deliberately left out

- **`GetUserInfo` still doesn't check who's calling.** Only the `ChangePassword` case (changing
  someone else's password) was closed, because it was the most serious; `GetUserInfo` exposes the
  email and stats of any user to anyone who knows their name. Same reason: the `.proto` carries no
  caller identity, so closing this properly would mean adding an auth field to the message or
  filtering in the Gateway — not done yet.
- **The `SendPasswordResetEmail` verification code still travels in the gRPC response**
  (`sent`, `code`), same as in the original `.proto`. It's the Gateway
  (`auth/AuthController.java`, in-memory map with a 10-minute TTL) that validates it before
  applying the password change, not this service.
- **The `NetworkPolicy` added in `k8s/07-user-service.yaml`** restricts which pod can call over
  the network, but doesn't replace real identity verification in the message — it's defense in
  depth, not the solution.

## How to run

Requires a reachable PostgreSQL instance (use `docker-compose up postgres` from
`anticheat-backend/`, or set `DB_HOST`/`DB_USER`/`DB_PASSWORD` to point at your own) — there's no
longer a fallback to a local H2 file. It also requires `anticheat-backend-libs` to be installed in
the local Maven repository (`cd ../anticheat-backend-libs && mvn install -DskipTests`) before
building this module.

```bash
mvn package
DB_HOST=localhost DB_USER=postgres DB_PASSWORD=postgres java -jar target/user-service-1.0-SNAPSHOT.jar
```

Requires JDK 21 (Temurin) and Maven on the PATH — installed on this machine at
`C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot` and `C:\Tools\apache-maven-3.9.16`.

The service listens for gRPC on port 9090 and Actuator (HTTP) on 9091. `smoke-test.ps1` only
checks `/actuator/health*` — PowerShell doesn't speak gRPC. To test the business contract
(register/login/etc.) use `mvn test` (in-process, no external dependencies) or a gRPC client like
`grpcurl` against port 9090.

```powershell
.\smoke-test.ps1
```

## Environment variables

| Variable | Description | Default |
|----------|-------------|-------------|
| `GRPC_PORT` | gRPC server port | `9090` |
| `HEALTH_PORT` | Actuator port (`/actuator/health*`) | `9091` |
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `anticheat` |
| `DB_USER` | Database user | `postgres` |
| `DB_PASSWORD` | Database password | *(empty)* |
| `SMTP_USER` / `SMTP_PASSWORD` | Gmail credentials for `EmailNotificationService` (mapped to `user-service.smtp-user`/`user-service.smtp-password` in `application.properties`) | *(no value: fails gracefully)* |

## What's missing (roadmap)

1. Bean Validation (`spring-boot-starter-validation` + `@NotBlank`/`@Size`) instead of the
   hand-rolled `isBlank(...)` in `UserGrpcService` — this would finally include a minimum
   password length policy in this service (the Gateway already validates a minimum length of 8 in
   `/auth/register` and `/auth/reset-password`, but `ChangePassword`/`ChangePasswordByEmail` don't
   go through that validation).
2. Close the access control gap in `GetUserInfo` (see "What was deliberately left out").
