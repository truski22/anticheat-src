# ws-lib (legacy - transitional)

> **Deprecated.** This module bootstraps a standalone Tyrus/`javax.websocket` server for
> the gateway's current WebSocket endpoint. It exists only because the gateway hasn't yet
> migrated onto `spring-boot-starter-websocket`. Once that migration lands, this module
> should be deleted, not refactored further - do not add new functionality here.

## Overview

Wraps the Glassfish Tyrus WebSocket server with a minimal lifecycle API. The only class
left in this module, `WebSocketServerBootstrap`, is a thin `start()`/`stop()` wrapper
around `org.glassfish.tyrus.server.Server`.

Everything else that previously lived here (a demo `@ServerEndpoint`, a JSON-B based
`Message`/`MessageEncoder`/`MessageDecoder` trio, and a `main()` entry point) was unused
dead code - not referenced by any consumer - and has been removed. The gateway's actual
production endpoint (`websocket.WebSocket` in `anticheat-backend-gateway`, with JWT
authentication) never used any of it; the real message (de)serialization for this system
lives in the [`protocol`](../protocol/README.md) module instead.

## Key Components

| Class | Purpose |
|---|---|
| `com.chessfraud.ws.WebSocketServerBootstrap` | Starts/stops a Tyrus server for a given endpoint class |

## Usage

```xml
<dependency>
    <groupId>com.chessfraud</groupId>
    <artifactId>ws-lib</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

```java
var server = new WebSocketServerBootstrap("0.0.0.0", 8080, "/", null, MyEndpoint.class);
server.start(); // throws IllegalStateException if the port can't be bound
```

## Why this is going away

Spring Boot has native WebSocket support (`spring-boot-starter-websocket`, already a
dependency of `anticheat-backend-gateway`) that supersedes a hand-rolled Tyrus bootstrap:
endpoint registration, lifecycle, and container integration all come for free. The
gateway's `WebSocket`/`ServerSocket` wiring predates that dependency being added and
hasn't been migrated yet - see the parent module's
[ARCHITECTURE.md](../ARCHITECTURE.md) for the migration plan.

## Known technical debt (do not "fix" without also migrating the gateway)

The `pom.xml` deliberately keeps both the `jakarta.websocket-api:2.1.1` and the legacy
`javax.websocket-api:1.1` / Tyrus `1.13` dependency lines side by side. The gateway's
endpoint code still compiles against `javax.websocket.*`. Removing either line without
first migrating that code to the `jakarta` namespace (or to Spring) will break the
gateway build.
