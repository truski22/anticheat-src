# Chess Fraud Detection — WebSocket Protocol

## Overview

The gateway bridges WebSocket connections to the internal MQTT bus. Clients authenticate via REST endpoints to obtain a JWT token, then connect via WebSocket at `/ws?token=JWT_TOKEN`. The username is extracted from the JWT — **not** included in the message envelope.

All messages use a typed envelope (`ChessMessage`) with a `type` discriminator, an optional `messageId` for request/response correlation, and a typed `payload`.

## Message Envelope

```json
{
  "type": "MESSAGE_TYPE",
  "messageId": "uuid-string",
  "payload": { ... }
}
```

| Field       | Type         | Required | Description                                      |
|-------------|--------------|----------|--------------------------------------------------|
| `type`      | `MessageType`| Yes      | Discriminator for the message kind                |
| `messageId` | `string`     | No       | UUID for request/response correlation             |
| `payload`   | `object`     | Depends  | Typed payload; some types have no payload         |

---

## Authentication

### REST Auth Endpoints (Port 8081)

Authentication is handled via REST endpoints. These return a JWT token on success.

| Endpoint               | Method | Body                                      | Auth | Description              |
|------------------------|--------|-------------------------------------------|------|--------------------------|
| `/auth/login`          | POST   | `{"user":"x","password":"y"}`             | No   | Login, returns JWT       |
| `/auth/register`       | POST   | `{"user":"x","email":"e","password":"y"}` | No   | Register, returns JWT    |
| `/auth/forgot-password`| POST   | `{"email":"x"}`                           | No   | Send password reset email|
| `/auth/reset-password` | POST   | `{"email":"x","password":"y"}`            | No   | Reset password via email |
| `/health`              | GET    | —                                         | No   | Health check             |

**Successful login/register response:**
```json
{
  "success": true,
  "message": "OK",
  "token": "eyJhbGciOiJIUzI1NiIs..."
}
```

### WebSocket Connection

After obtaining a JWT token, connect to WebSocket:

```
ws://host:8080/ws?token=YOUR_JWT_TOKEN
```

Connections without a valid token are immediately closed with policy violation.

### Rate Limiting

Each WebSocket session is rate-limited to **10 messages per second**. If exceeded:
1. An `ERROR` message is sent with code `RATE_LIMIT_EXCEEDED`
2. The connection is closed

```json
{
  "type": "ERROR",
  "payload": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Too many messages. Max 10 per second."
  }
}
```

---

## Message Types

### Client → Gateway

> **Note:** `LOGIN`, `REGISTER`, `SEND_EMAIL_CHANGE_PASSWORD`, and `CHANGE_PASSWORD_EMAIL` have been moved to REST endpoints (see [Authentication](#authentication) above). They are no longer accepted over WebSocket.

| Type                        | Auth Required | Payload                             | MQTT Topic              | Description                        |
|-----------------------------|---------------|--------------------------------------|-------------------------|------------------------------------|
| `USER_INFO_REQUEST`         | Yes           | _(none)_                             | `user/info`             | Request user profile info          |
| `GAMES_REQUEST`             | Yes           | _(none)_                             | `game/info`             | Request list of user's games       |
| `ANALYZE_GAME`              | Yes           | `AnalyzeGamePayload`                 | `game/analyze`          | Submit game for fraud analysis     |
| `SAVE_GAME`                 | Yes           | `SaveGamePayload`                    | `game/info`             | Save a completed game              |
| `CHANGE_PASSWORD`           | Yes           | `ChangePasswordPayload`              | `user/password`         | Change password (authenticated)    |

### Gateway → Client

| Type                              | Payload                                | Description                        |
|-----------------------------------|----------------------------------------|------------------------------------|
| `LOGIN_RESPONSE`                  | `ResponsePayload`                      | Login result                       |
| `REGISTER_RESPONSE`               | `ResponsePayload`                      | Registration result                |
| `USER_INFO`                       | `UserInfoPayload`                      | User profile data                  |
| `GAMES`                           | `GamesPayload`                         | List of user's games               |
| `ANALYZE_RESULT`                  | `AnalyzeResultPayload`                 | Fraud analysis result              |
| `SAVE_GAME_RESPONSE`              | `ResponsePayload`                      | Save game result                   |
| `CHANGE_PASSWORD_EMAIL_RESPONSE`  | `ChangePasswordEmailResponsePayload`   | Password reset code                |
| `ERROR`                           | `ErrorPayload`                         | Error with code and message        |

### Future Types

| Type          | Payload              | Description                        |
|---------------|----------------------|------------------------------------|
| `MOVE`        | `MovePayload`        | Live game move                     |
| `RESIGN`      | _(TBD)_              | Player resignation                 |
| `CHAT`        | _(TBD)_              | In-game chat                       |
| `FRAUD_ALERT` | `FraudAlertPayload`  | Real-time fraud detection alert    |

---

## Payload Schemas

### LoginPayload
| Field      | Type     | Required | Validation          |
|------------|----------|----------|---------------------|
| `password` | `string` | Yes      | Non-null, non-blank |

### RegisterPayload
| Field      | Type     | Required | Validation          |
|------------|----------|----------|---------------------|
| `email`    | `string` | Yes      | Non-null, non-blank |
| `password` | `string` | Yes      | Non-null, non-blank |

### AnalyzeGamePayload
| Field   | Type     | Required | Validation          |
|---------|----------|----------|---------------------|
| `moves` | `string` | Yes      | Non-null, non-blank |

### SaveGamePayload
| Field   | Type      | Required | Validation          |
|---------|-----------|----------|---------------------|
| `moves` | `string`  | Yes      | Non-null, non-blank |
| `legal` | `boolean` | Yes      | —                   |

### ChangePasswordPayload
| Field      | Type     | Required | Validation          |
|------------|----------|----------|---------------------|
| `password` | `string` | Yes      | Non-null, non-blank |

### ChangePasswordEmailPayload
| Field      | Type     | Required | Validation          |
|------------|----------|----------|---------------------|
| `email`    | `string` | Yes      | Non-null, non-blank |
| `password` | `string` | Yes      | Non-null, non-blank |

### SendEmailChangePasswordPayload
| Field   | Type     | Required | Validation          |
|---------|----------|----------|---------------------|
| `email` | `string` | Yes      | Non-null, non-blank |

### ResponsePayload
| Field     | Type      | Description              |
|-----------|-----------|--------------------------|
| `success` | `boolean` | Whether the action succeeded |
| `message` | `string`  | Human-readable message   |

### UserInfoPayload
| Field        | Type     | Description                 |
|--------------|----------|-----------------------------|
| `email`      | `string` | User's email                |
| `totalGames` | `int`    | Total games played          |
| `cheatGames` | `int`    | Games flagged as cheating   |
| `legalGames` | `int`    | Games confirmed as legal    |

### GamesPayload
| Field   | Type           | Description            |
|---------|----------------|------------------------|
| `games` | `GameEntry[]`  | List of game entries   |

**GameEntry:**
| Field   | Type      | Description                |
|---------|-----------|----------------------------|
| `moves` | `string`  | Move notation string       |
| `legal` | `boolean` | Whether the game was legal |

### AnalyzeResultPayload
| Field   | Type       | Description                              |
|---------|------------|------------------------------------------|
| `legal` | `boolean`  | Whether the game is classified as legal  |
| `white` | `int[]`    | Engine eval centipawns for white moves   |
| `black` | `int[]`    | Engine eval centipawns for black moves   |

### ChangePasswordEmailResponsePayload
| Field  | Type     | Description            |
|--------|----------|------------------------|
| `code` | `string` | Verification code sent |

### ErrorPayload
| Field     | Type     | Description          |
|-----------|----------|----------------------|
| `code`    | `string` | Machine-readable code|
| `message` | `string` | Human-readable error |

### MovePayload _(future)_
| Field       | Type     | Description              |
|-------------|----------|--------------------------|
| `from`      | `string` | Source square (e.g. "e2")|
| `to`        | `string` | Target square (e.g. "e4")|
| `player`    | `string` | Player making the move   |
| `timestamp` | `long`   | Unix timestamp in ms     |

### FraudAlertPayload _(future)_
| Field              | Type     | Description                     |
|--------------------|----------|---------------------------------|
| `gameId`           | `string` | ID of the flagged game          |
| `fraudProbability` | `double` | Probability score (0.0 - 1.0)  |
| `reason`           | `string` | Explanation of the flag         |

---

## Example Messages

### Login
```json
{
  "type": "LOGIN",
  "messageId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "payload": {
    "password": "secret123"
  }
}
```

### Login Response
```json
{
  "type": "LOGIN_RESPONSE",
  "messageId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "payload": {
    "success": true,
    "message": "Authenticated"
  }
}
```

### Register
```json
{
  "type": "REGISTER",
  "messageId": "f1e2d3c4-b5a6-7890-1234-567890abcdef",
  "payload": {
    "email": "player@example.com",
    "password": "strongPassword1!"
  }
}
```

### User Info Request (no payload)
```json
{
  "type": "USER_INFO_REQUEST",
  "messageId": "11111111-2222-3333-4444-555555555555"
}
```

### User Info Response
```json
{
  "type": "USER_INFO",
  "messageId": "11111111-2222-3333-4444-555555555555",
  "payload": {
    "email": "player@example.com",
    "totalGames": 42,
    "cheatGames": 2,
    "legalGames": 40
  }
}
```

### Analyze Game
```json
{
  "type": "ANALYZE_GAME",
  "messageId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "payload": {
    "moves": "e2e4 e7e5 g1f3 b8c6 f1b5"
  }
}
```

### Analyze Result
```json
{
  "type": "ANALYZE_RESULT",
  "messageId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "payload": {
    "legal": true,
    "white": [20, 15, 25],
    "black": [-10, -5, -20]
  }
}
```

### Save Game
```json
{
  "type": "SAVE_GAME",
  "messageId": "bbbbbbbb-cccc-dddd-eeee-ffffffffffff",
  "payload": {
    "moves": "e2e4 e7e5 g1f3 b8c6 f1b5",
    "legal": true
  }
}
```

### Games List
```json
{
  "type": "GAMES",
  "messageId": "cccccccc-dddd-eeee-ffff-000000000000",
  "payload": {
    "games": [
      { "moves": "e2e4 e7e5 g1f3", "legal": true },
      { "moves": "d2d4 d7d5 c2c4", "legal": false }
    ]
  }
}
```

### Change Password
```json
{
  "type": "CHANGE_PASSWORD",
  "messageId": "dddddddd-eeee-ffff-0000-111111111111",
  "payload": {
    "password": "newSecurePassword!"
  }
}
```

### Send Email Change Password
```json
{
  "type": "SEND_EMAIL_CHANGE_PASSWORD",
  "messageId": "eeeeeeee-ffff-0000-1111-222222222222",
  "payload": {
    "email": "player@example.com"
  }
}
```

### Change Password via Email
```json
{
  "type": "CHANGE_PASSWORD_EMAIL",
  "messageId": "ffffffff-0000-1111-2222-333333333333",
  "payload": {
    "email": "player@example.com",
    "password": "newPassword123!"
  }
}
```

### Change Password Email Response
```json
{
  "type": "CHANGE_PASSWORD_EMAIL_RESPONSE",
  "messageId": "ffffffff-0000-1111-2222-333333333333",
  "payload": {
    "code": "A3F8K2"
  }
}
```

### Error
```json
{
  "type": "ERROR",
  "messageId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "payload": {
    "code": "INVALID_PAYLOAD",
    "message": "Password is required"
  }
}
```

### Move _(future)_
```json
{
  "type": "MOVE",
  "messageId": "00000000-1111-2222-3333-444444444444",
  "payload": {
    "from": "e2",
    "to": "e4",
    "player": "white",
    "timestamp": 1700000000000
  }
}
```

### Fraud Alert _(future)_
```json
{
  "type": "FRAUD_ALERT",
  "messageId": "55555555-6666-7777-8888-999999999999",
  "payload": {
    "gameId": "game-abc-123",
    "fraudProbability": 0.92,
    "reason": "Centipawn loss consistently below 10 across 40 moves"
  }
}
```

---

## Error Handling

The gateway returns `ERROR` messages with structured error codes:

| Code                  | Description                                       |
|-----------------------|---------------------------------------------------|
| `MISSING_TYPE`        | Message JSON missing the `type` field             |
| `UNKNOWN_TYPE`        | Unrecognized `MessageType` value                  |
| `MISSING_PAYLOAD`     | Type requires a payload but none was provided     |
| `INVALID_PAYLOAD`     | Payload failed validation or deserialization       |
| `MALFORMED_JSON`      | Message could not be parsed as JSON               |
| `RATE_LIMIT_EXCEEDED` | Client exceeded 10 messages/second limit          |
| `UNAUTHORIZED_TYPE`   | Message type not allowed over WebSocket           |

The gateway uses `PayloadRegistry.deserialize()` to parse incoming messages. If deserialization fails, an `ERROR` `ChessMessage` is sent back with the `messageId` of the original request (if available).

---

## MQTT Topic Mapping

The gateway maps WebSocket messages to internal MQTT topics:

| MQTT Topic       | Message Types Routed                                                                 |
|------------------|--------------------------------------------------------------------------------------|
| `user/info`      | `USER_INFO_REQUEST` → `USER_INFO`; Login/Register via REST → `LOGIN_RESPONSE`, `REGISTER_RESPONSE` |
| `user/password`  | `CHANGE_PASSWORD`; Forgot/Reset password via REST                                    |
| `game/info`      | `GAMES_REQUEST`, `SAVE_GAME` → `GAMES`, `SAVE_GAME_RESPONSE`                        |
| `game/analyze`   | `ANALYZE_GAME` → `ANALYZE_RESULT`                                                   |

The username is extracted from the JWT token during WebSocket handshake and injected into the MQTT message context — it is never part of the `ChessMessage` envelope.
