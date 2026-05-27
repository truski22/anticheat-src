# WebSocket Library

WebSocket server library used by the gateway service.

## Overview

Wraps the Glassfish Tyrus WebSocket server, providing a simplified API for WebSocket endpoint management with built-in message encoding/decoding.

## Key Components

| Class | Purpose |
|-------|---------|
| `ServerSocket` | WebSocket server bootstrap and lifecycle |
| `BaseWebSocket` | Base class for WebSocket endpoints |
| `Message` | Typed message wrapper |
| `MessageEncoder` | Serializes outbound messages |
| `MessageDecoder` | Deserializes inbound messages |

## Usage

This is **not a standalone service**. Add it as a Maven dependency:

```bash
mvn -f ws-lib/pom.xml install
```

Then reference it in your service's `pom.xml`:

```xml
<dependency>
    <groupId>com.tfm</groupId>
    <artifactId>ws-lib</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## Technology

- Glassfish Tyrus WebSocket Server
