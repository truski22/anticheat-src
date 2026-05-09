# Shared

Common data models, utilities, and configuration used by all Java microservices.

## Overview

Contains the MQTT message DTOs, database utilities, JSON helpers, and per-service MQTT configuration files that form the shared contract across all services.

## Key Components

### Message DTOs

| DTO | Used By |
|-----|---------|
| `LoginRequest` / `LoginResponse` | user-service, gateway |
| `RegisterRequest` / `RegisterResponse` | user-service, gateway |
| `UserInfoRequest` / `UserInfoResponse` | user-service, gateway |
| `ChangePassword` / `ChangePasswordSendEmail` | user-service, gateway |
| `GamesRequest` / `GamesResponse` | game-service, gateway |
| `SaveGame` | game-service, gateway |
| `AnalyzeGame` / `AnalyzeGameResponse` | analysis-service, gateway |

### Utilities

| Class | Purpose |
|-------|---------|
| `Database` | MySQL connection factory |
| `Utils` | JSON parsing helpers (Jackson) |

### Configuration

MQTT `.properties` files for each service (broker URL, client ID, topics).

## Usage

This is **not a standalone service**. Add it as a Maven dependency:

```bash
mvn -f shared/pom.xml install
```

Then reference it in your service's `pom.xml`:

```xml
<dependency>
    <groupId>com.tfm</groupId>
    <artifactId>shared</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```
