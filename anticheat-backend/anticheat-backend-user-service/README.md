# User Service

User management and authentication service, communicating over MQTT.

## Overview

Handles user registration, login, profile queries, and password management. Supports email-based password reset via SMTP (Gmail).

## MQTT Interface

| Topic | Direction | Messages |
|-------|-----------|----------|
| `user/info` | Subscribe | `LoginRequest`, `RegisterRequest`, `UserInfoRequest` |
| `user/password` | Subscribe | `ChangePassword`, `ChangePasswordSendEmail` |

## Message Handling

| Message | Description |
|---------|-------------|
| `LoginRequest` | Authenticate user credentials |
| `RegisterRequest` | Create a new user account |
| `UserInfoRequest` | Retrieve user profile and stats |
| `ChangePassword` | Update password with verification code |
| `ChangePasswordSendEmail` | Send password reset code via email |

## Database

- **Database:** `anticheat_users`
- **Table:** `users`

| Column | Description |
|--------|-------------|
| `name` | Username |
| `email` | User email address |
| `password` | Hashed password |
| `total_games` | Total games played |
| `cheated_games` | Games flagged as cheated |
| `fair_games` | Games flagged as fair |

## Environment Variables

| Variable | Description |
|----------|-------------|
| `MYSQL_HOST` | MySQL server hostname |
| `MYSQL_USER` | Database username |
| `MYSQL_PASSWORD` | Database password |

## Requirements

- Java 11+, Maven 3+
- MySQL with `anticheat_users` database
- MQTT broker
- Gmail SMTP credentials (for password reset emails)

## How to Run

```bash
mvn -f shared/pom.xml install
mvn -f mqtt-lib/pom.xml install
mvn -f user-service/pom.xml package
java -jar user-service/target/user-service-1.0-SNAPSHOT.jar
```

No exposed ports — communicates only via MQTT.
