-- Copy of anticheat-backend-infra/docker/init.sql — not executed automatically
-- (spring.sql.init.mode=never): in Postgres the real schema is created by
-- postgres-init-config when the StatefulSet starts, not by this app.
-- Only for spinning up a local database without docker-compose: mvn spring-boot:run
-- -Dspring-boot.run.arguments=--spring.sql.init.mode=always
CREATE TABLE IF NOT EXISTS users (
    name          VARCHAR(255) PRIMARY KEY,
    email         VARCHAR(255) UNIQUE NOT NULL,
    password      VARCHAR(255) NOT NULL,
    total_games   INT DEFAULT 0,
    cheated_games INT DEFAULT 0,
    fair_games    INT DEFAULT 0
);
