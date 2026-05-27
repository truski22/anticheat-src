package service;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Provides account-management operations backed by the relational database.
 * All methods handle their own {@link SQLException} internally and log errors
 * via SLF4J; callers receive safe result objects or {@code boolean} flags.
 */
public class UserAccountService {

    private static final Logger log = LoggerFactory.getLogger(UserAccountService.class);

    private final Database database;

    /**
     * @param database shared database connection pool
     */
    public UserAccountService(Database database) {
        this.database = database;
    }

    /**
     * Validates credentials against the stored BCrypt hash.
     *
     * @param username the account name
     * @param password the plaintext password supplied by the client
     * @return {@link LoginResult} with {@code "OK"} on success, {@code "KO"} otherwise
     */
    public LoginResult login(String username, String password) {
        String query = "SELECT password FROM users WHERE name = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    String response = BCrypt.checkpw(password, storedHash) ? "OK" : "KO";
                    return new LoginResult(response);
                }
                return new LoginResult("KO");
            }
        } catch (SQLException e) {
            log.error("[USER-SERVICE] DB error in login for user '{}': {}", username, e.getMessage(), e);
            return new LoginResult("KO");
        }
    }

    /**
     * Creates a new user account with a BCrypt-hashed password.
     *
     * @param username desired username
     * @param email    user's e-mail address
     * @param password plaintext password chosen by the user
     * @return {@link RegisterResult} with code {@code "OK"}, {@code "UNV"} (username taken),
     *         {@code "ENV"} (e-mail taken), or {@code "KO"} (unexpected error)
     */
    public RegisterResult register(String username, String email, String password) {
        String queryName  = "SELECT COUNT(*) FROM users WHERE name = ?";
        String queryEmail = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement pstName  = connection.prepareStatement(queryName);
             PreparedStatement pstEmail = connection.prepareStatement(queryEmail)) {
            pstName.setString(1, username);
            pstEmail.setString(1, email);
            try (ResultSet rsName  = pstName.executeQuery();
                 ResultSet rsEmail = pstEmail.executeQuery()) {
                if (rsName.next() && rsName.getInt(1) != 0) {
                    return new RegisterResult("UNV");
                }
                if (rsEmail.next() && rsEmail.getInt(1) != 0) {
                    return new RegisterResult("ENV");
                }
                String insertQuery =
                        "INSERT INTO users (name,email,password,total_games,cheated_games,fair_games) VALUES (?,?,?,?,?,?)";
                try (PreparedStatement pstInsert = connection.prepareStatement(insertQuery)) {
                    String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
                    pstInsert.setString(1, username);
                    pstInsert.setString(2, email);
                    pstInsert.setString(3, hashedPassword);
                    pstInsert.setInt(4, 0);
                    pstInsert.setInt(5, 0);
                    pstInsert.setInt(6, 0);
                    pstInsert.executeUpdate();
                    return new RegisterResult("OK");
                }
            }
        } catch (SQLException e) {
            log.error("[USER-SERVICE] DB error in register for user '{}': {}", username, e.getMessage(), e);
            return new RegisterResult("KO");
        }
    }

    /**
     * Retrieves profile statistics for a user.
     *
     * @param username the account name to look up
     * @return a populated {@link UserInfoResult}, or {@code null} if the user does not exist
     */
    public UserInfoResult getUserInfo(String username) {
        String query = "SELECT email, total_games, cheated_games, fair_games FROM users WHERE name = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, username);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return new UserInfoResult(
                            rs.getString("email"),
                            rs.getInt("total_games"),
                            rs.getInt("cheated_games"),
                            rs.getInt("fair_games"));
                }
            }
        } catch (SQLException e) {
            log.error("[USER-SERVICE] DB error in getUserInfo for user '{}': {}", username, e.getMessage(), e);
        }
        return null;
    }

    /**
     * Updates the password for a given username.
     *
     * @param username    the account whose password should change
     * @param newPassword the new plaintext password (will be BCrypt-hashed)
     * @return {@code true} if the update affected at least one row
     */
    public boolean changePassword(String username, String newPassword) {
        String checkQuery  = "SELECT COUNT(*) FROM users WHERE name = ?";
        String updateQuery = "UPDATE users SET password = ? WHERE name = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
            checkStmt.setString(1, username);
            try (ResultSet rs = checkStmt.executeQuery()) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    return false;
                }
            }
            try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                updateStmt.setString(1, BCrypt.hashpw(newPassword, BCrypt.gensalt(12)));
                updateStmt.setString(2, username);
                return updateStmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            log.error("[USER-SERVICE] DB error in changePassword for user '{}': {}", username, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Updates the password for the account associated with the given e-mail address.
     *
     * @param email       the account's e-mail address
     * @param newPassword the new plaintext password (will be BCrypt-hashed)
     * @return {@code true} if the update affected at least one row
     */
    public boolean changePasswordByEmail(String email, String newPassword) {
        String checkQuery  = "SELECT COUNT(*) FROM users WHERE email = ?";
        String updateQuery = "UPDATE users SET password = ? WHERE email = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
            checkStmt.setString(1, email);
            try (ResultSet rs = checkStmt.executeQuery()) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    return false;
                }
            }
            try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                updateStmt.setString(1, BCrypt.hashpw(newPassword, BCrypt.gensalt(12)));
                updateStmt.setString(2, email);
                return updateStmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            log.error("[USER-SERVICE] DB error in changePasswordByEmail for email '{}': {}", email, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checks whether a username is already registered.
     *
     * @param username the username to check
     * @return {@code true} if the username exists
     */
    public boolean usernameExists(String username) {
        String query = "SELECT COUNT(*) FROM users WHERE name = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, username);
            try (ResultSet rs = pst.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            log.error("[USER-SERVICE] DB error in usernameExists for '{}': {}", username, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checks whether an e-mail address is already registered.
     *
     * @param email the e-mail address to check
     * @return {@code true} if the e-mail exists
     */
    public boolean emailExists(String email) {
        String query = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, email);
            try (ResultSet rs = pst.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            log.error("[USER-SERVICE] DB error in emailExists for '{}': {}", email, e.getMessage(), e);
            return false;
        }
    }
}
