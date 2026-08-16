# grpc-api

Generated gRPC client/server stubs for the internal service-to-service contracts:
`AnalysisService`, `GameService`, `UserService`.

## Overview

Source of truth is the `.proto` files under `src/main/proto`; Java stubs are generated
at build time by the `protobuf-maven-plugin` (nothing under a generated-sources
directory is hand-edited or committed).

| Proto | Service | Package |
|---|---|---|
| `analysis_service.proto` | `AnalysisService` (`AnalyzeGame`) | `com.chessfraud.grpc.analysis` |
| `game_service.proto` | `GameService` (`GetGames`, `SaveGame`) | `com.chessfraud.grpc.game` |
| `user_service.proto` | `UserService` (`Login`, `Register`, `GetUserInfo`, `ChangePassword`, ...) | `com.chessfraud.grpc.user` |

## Consumers

- `gateway` - client stubs, via `grpc.ServiceClients`
- `user-service` - server implementation, via `net.devh:grpc-server-spring-boot-starter`
  and `UserGrpcService`
- `game-service`, `analysis-service` - server implementations (plain gRPC server today;
  candidates for `grpc-server-spring-boot-starter` once migrated to Spring Boot, matching
  `user-service`'s pattern)

## Usage

```xml
<dependency>
    <groupId>com.chessfraud</groupId>
    <artifactId>grpc-api</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

Keep `grpc.version` here in sync with whatever runtime a consumer pins (see
`user-service`'s `pom.xml` comment on its `grpc-bom` import) to avoid binary mismatches
between the generated stubs and the gRPC runtime a service ships.
