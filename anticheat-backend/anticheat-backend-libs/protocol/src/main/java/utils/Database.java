package utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class Database {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Database.class);
    private final HikariDataSource dataSource;

    public Database() {
        String host     = System.getenv().getOrDefault("DB_HOST", "localhost");
        String dbName   = System.getenv().getOrDefault("DB_NAME", "anticheat");
        String user     = System.getenv().getOrDefault("DB_USER", "postgres");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "postgres");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://" + host + ":5432/" + dbName);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(config);
        log.info("[DB] Connection pool initialized for {} on {}", dbName, host);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * @deprecated DB name is now configured via DB_NAME env var. Use {@link #getConnection()} instead.
     */
    @Deprecated
    public Connection getConnection(String database) throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("[DB] Connection pool closed.");
        }
    }

    /**
     * @deprecated Connections obtained from the pool must be closed directly (try-with-resources).
     * Calling conn.close() returns them to the pool automatically.
     */
    @Deprecated
    public void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                log.error("[DB] Error returning connection to pool: {}", e.getMessage(), e);
            }
        }
    }
}
