# protocol

Framework-agnostic wire protocol between WebSocket clients and the gateway.

## Overview

Defines the `ChessMessage` envelope exchanged over the gateway's WebSocket connection,
the catalog of message types, the typed payload for each one, and the `PayloadRegistry`
that (de)serializes between raw JSON and these types.

This module has no dependency on any web/application framework - only Jackson and the
SLF4J API - so it drops into a Spring MVC / `spring-boot-starter-websocket` component
(see the parent module's [ARCHITECTURE.md](../ARCHITECTURE.md)) without changes. The
gateway's `TextWebSocketHandler` is the only current consumer.

## Package layout

| Package | Contents |
|---|---|
| `com.chessfraud.protocol` | `ChessMessage` envelope, `MessageType` enum, `PayloadRegistry` |
| `com.chessfraud.protocol.payload` | One record per message payload (see catalog below) |
| `com.chessfraud.protocol.exception` | `InvalidMessageException` |

## Message catalog

| `MessageType` | Direction | Payload |
|---|---|---|
| `LOGIN` | Client → Gateway | `LoginPayload` |
| `REGISTER` | Client → Gateway | `RegisterPayload` |
| `USER_INFO_REQUEST` | Client → Gateway | *(none)* |
| `GAMES_REQUEST` | Client → Gateway | *(none)* |
| `ANALYZE_GAME` | Client → Gateway | `AnalyzeGamePayload` |
| `SAVE_GAME` | Client → Gateway | `SaveGamePayload` |
| `CHANGE_PASSWORD` | Client → Gateway | `ChangePasswordPayload` |
| `CHANGE_PASSWORD_EMAIL` | Client → Gateway | `ChangePasswordEmailPayload` |
| `SEND_EMAIL_CHANGE_PASSWORD` | Client → Gateway | `SendEmailChangePasswordPayload` |
| `LOGIN_RESPONSE` / `REGISTER_RESPONSE` / `SAVE_GAME_RESPONSE` / `CHANGE_PASSWORD_RESPONSE` | Gateway → Client | `ResponsePayload` |
| `USER_INFO` | Gateway → Client | `UserInfoPayload` |
| `GAMES` | Gateway → Client | `GamesPayload` |
| `ANALYZE_RESULT` | Gateway → Client | `AnalyzeResultPayload` |
| `CHANGE_PASSWORD_EMAIL_RESPONSE` | Gateway → Client | `ChangePasswordEmailResponsePayload` |
| `ERROR` | Gateway → Client | `ErrorPayload` |
| `MOVE`, `RESIGN`, `CHAT`, `FRAUD_ALERT` | Reserved for future game types | `MovePayload` / `FraudAlertPayload` (others unregistered) |

Adding a new type requires a matching entry in `PayloadRegistry`'s static registry block
unless the message intentionally carries no payload.

## Security hardening

`PayloadRegistry` is the trust boundary for every inbound WebSocket frame - untrusted
client input is parsed here before any business logic sees it:

- **Size cap** - `PayloadRegistry.MAX_MESSAGE_BYTES` (256 KiB) rejects oversized frames
  with `MESSAGE_TOO_LARGE` before parsing starts.
- **Parser constraints** - Jackson's `StreamReadConstraints` caps JSON nesting depth and
  string length, preventing stack-overflow / allocation-based DoS from a single crafted
  frame (deeply nested arrays, multi-megabyte strings).
- **No polymorphic type handling** - the payload class for a message is resolved from a
  fixed, compile-time `Map<MessageType, Class<?>>`, never from client-supplied type
  information, so this module is not exposed to Jackson deserialization-gadget attacks.
- **Fail-closed validation** - payload records validate their own invariants in compact
  constructors (e.g. `LoginPayload` rejects a blank password), so a malformed payload
  never reaches business logic as a half-populated object.

## Usage

Not a standalone service - add as a Maven dependency:

```xml
<dependency>
    <groupId>com.chessfraud</groupId>
    <artifactId>protocol</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

```java
ChessMessage message = PayloadRegistry.deserialize(rawJson); // throws InvalidMessageException
String json = PayloadRegistry.serialize(ChessMessage.error("RATE_LIMIT_EXCEEDED", "Too many messages"));
```

This module intentionally depends on `slf4j-api` only (not `logback-classic`) at compile
time - the consuming application chooses its own logging backend. `logback-classic` is a
test-only dependency here, used solely to see log output while running this module's own
tests.
