# MQTT Library

Shared MQTT abstraction library used by all Java microservices.

## Overview

Provides a high-level `MqttInterface` facade over the Eclipse Paho MQTT client, handling connection management, automatic reconnection, and JSON message serialization via Jackson.

## Key Components

| Class | Purpose |
|-------|---------|
| `MqttInterface` | Main facade — publish, subscribe, and connection lifecycle |
| Configuration | Driven by `.properties` files per service |

## Features

- Publish/subscribe with typed message objects
- Automatic JSON serialization/deserialization (Jackson)
- Configurable via `.properties` files
- Automatic reconnection handling

## Usage

This is **not a standalone service**. Add it as a Maven dependency:

```bash
mvn -f mqtt-lib/pom.xml install
```

Then reference it in your service's `pom.xml`:

```xml
<dependency>
    <groupId>com.tfm</groupId>
    <artifactId>mqtt-lib</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## Technology

- Eclipse Paho MQTT Client
- Jackson for JSON processing
