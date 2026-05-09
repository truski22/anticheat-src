-- PostgreSQL init script
-- Creates schemas and tables for the chess fraud detection system

-- Users database tables (in anticheat database)
CREATE TABLE IF NOT EXISTS users (
    name          VARCHAR(255) PRIMARY KEY,
    email         VARCHAR(255) UNIQUE NOT NULL,
    password      VARCHAR(255) NOT NULL,
    total_games   INT DEFAULT 0,
    cheated_games INT DEFAULT 0,
    fair_games    INT DEFAULT 0
);

-- Games table
CREATE TABLE IF NOT EXISTS games (
    id       SERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    moves    TEXT         NOT NULL,
    legal    BOOLEAN      NOT NULL
);
