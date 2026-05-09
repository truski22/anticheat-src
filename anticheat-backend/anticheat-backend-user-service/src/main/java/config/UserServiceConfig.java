package config;

/**
 * Configuration holder for the user-service, populated from environment variables.
 * Use {@link #fromEnvironment()} to build an instance at startup.
 */
public class UserServiceConfig {

    private final String smtpUser;
    private final String smtpPassword;
    private final String emailImagePath;
    private final String dbHost;
    private final String dbUser;
    private final String dbPassword;

    private UserServiceConfig(
            String smtpUser,
            String smtpPassword,
            String emailImagePath,
            String dbHost,
            String dbUser,
            String dbPassword) {
        this.smtpUser = smtpUser;
        this.smtpPassword = smtpPassword;
        this.emailImagePath = emailImagePath;
        this.dbHost = dbHost;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    /**
     * Creates a {@code UserServiceConfig} by reading the following environment variables:
     * <ul>
     *   <li>{@code SMTP_USER} – Gmail account used to send e-mails</li>
     *   <li>{@code SMTP_PASSWORD} – App password for the Gmail account</li>
     *   <li>{@code EMAIL_IMAGE_PATH} – Path to the inline image attached to verification e-mails
     *       (default: {@code chessfraud-libs/protocol/src/main/images/has.png})</li>
     *   <li>{@code DB_HOST} – Database host</li>
     *   <li>{@code DB_USER} – Database username</li>
     *   <li>{@code DB_PASSWORD} – Database password</li>
     * </ul>
     *
     * @return a fully populated {@code UserServiceConfig}
     */
    public static UserServiceConfig fromEnvironment() {
        String smtpUser     = System.getenv("SMTP_USER");
        String smtpPassword = System.getenv("SMTP_PASSWORD");
        String imagePath    = System.getenv().getOrDefault(
                "EMAIL_IMAGE_PATH",
                "chessfraud-libs/protocol/src/main/images/has.png");
        String dbHost       = System.getenv("DB_HOST");
        String dbUser       = System.getenv("DB_USER");
        String dbPassword   = System.getenv("DB_PASSWORD");
        return new UserServiceConfig(smtpUser, smtpPassword, imagePath, dbHost, dbUser, dbPassword);
    }

    /** @return SMTP username (Gmail address) */
    public String getSmtpUser() { return smtpUser; }

    /** @return SMTP application password */
    public String getSmtpPassword() { return smtpPassword; }

    /** @return Filesystem path to the inline e-mail image */
    public String getEmailImagePath() { return emailImagePath; }

    /** @return Database host */
    public String getDbHost() { return dbHost; }

    /** @return Database username */
    public String getDbUser() { return dbUser; }

    /** @return Database password */
    public String getDbPassword() { return dbPassword; }
}
