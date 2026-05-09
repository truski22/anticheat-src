# Game Service

Persistence service for chess game records, communicating exclusively over MQTT.

## Overview

Subscribes to the `game/info` MQTT topic and handles two operations: querying games by username and saving new game records to MySQL.

## MQTT Interface

| Topic | Direction | Messages |
|-------|-----------|----------|
| `game/info` | Subscribe | `GamesRequest`, `SaveGame` |
| `game/info` | Publish | Response with game list or save confirmation |

## Message Handling

| Message | Operation | Description |
|---------|-----------|-------------|
| `GamesRequest` | `SELECT` | Retrieves all games for a given username |
| `SaveGame` | `INSERT` | Persists a new game record |

## Database

- **Database:** `anticheat_games`
- **Table:** `games`

| Column | Description |
|--------|-------------|
| `id` | Primary key |
| `username` | Player identifier |
| `moves` | Game move sequence |
| `legal` | Whether the game was flagged as fair |

## Environment Variables

| Variable | Description |
|----------|-------------|
| `MYSQL_HOST` | MySQL server hostname |
| `MYSQL_USER` | Database username |
| `MYSQL_PASSWORD` | Database password |

## Requirements

- Java 11+, Maven 3+
- MySQL with `anticheat_games` database
- MQTT broker

## How to Run

```bash
mvn -f shared/pom.xml install
mvn -f mqtt-lib/pom.xml install
mvn -f game-service/pom.xml package
java -jar game-service/target/game-service-1.0-SNAPSHOT.jar
```

No exposed ports — communicates only via MQTT.
