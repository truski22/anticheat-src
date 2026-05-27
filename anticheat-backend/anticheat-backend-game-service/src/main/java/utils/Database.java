package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private static final Logger log = LoggerFactory.getLogger(Database.class);
    private final String jdbcUrl;
    private final String user;
    private final String password;

    public Database() {
        String host = System.getenv().getOrDefault("DB_HOST", "localhost");
        String dbName = System.getenv().getOrDefault("DB_NAME", "anticheat");
        this.user = System.getenv().getOrDefault("DB_USER", "postgres");
        this.password = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        this.jdbcUrl = "jdbc:postgresql://" + host + ":5432/" + dbName;
        log.info("[DB] Database client configured for {} on {}", dbName, host);
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, user, password);
    }

    @Deprecated
    public Connection getConnection(String database) throws SQLException {
        return getConnection();
    }

    public void close() {
        log.info("[DB] Database client closed.");
    }

    @Deprecated
    public void closeConnection(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            log.error("[DB] Error closing connection: {}", e.getMessage(), e);
        }
    }
}
