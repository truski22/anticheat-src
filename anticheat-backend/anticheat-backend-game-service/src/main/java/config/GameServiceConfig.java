package config;

/**
 * Configuration holder for the game-service, populated from environment variables.
 * Use {@link #fromEnvironment()} to build an instance at startup.
 */
public class GameServiceConfig {

    private final String dbHost;
    private final String dbUser;
    private final String dbPassword;

    private GameServiceConfig(String dbHost, String dbUser, String dbPassword) {
        this.dbHost     = dbHost;
        this.dbUser     = dbUser;
        this.dbPassword = dbPassword;
    }

    /**
     * Creates a {@code GameServiceConfig} by reading the following environment variables:
     * <ul>
     *   <li>{@code DB_HOST}     – Database host</li>
     *   <li>{@code DB_USER}     – Database username</li>
     *   <li>{@code DB_PASSWORD} – Database password</li>
     * </ul>
     *
     * @return a fully populated {@code GameServiceConfig}
     */
    public static GameServiceConfig fromEnvironment() {
        return new GameServiceConfig(
                System.getenv("DB_HOST"),
                System.getenv("DB_USER"),
                System.getenv("DB_PASSWORD"));
    }

    /** @return database host */
    public String getDbHost() { return dbHost; }

    /** @return database username */
    public String getDbUser() { return dbUser; }

    /** @return database password */
    public String getDbPassword() { return dbPassword; }
}
