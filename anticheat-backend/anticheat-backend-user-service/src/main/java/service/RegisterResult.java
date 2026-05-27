package service;

/**
 * Represents the outcome of a registration attempt.
 *
 * <ul>
 *   <li>{@code "OK"}  – registration successful</li>
 *   <li>{@code "UNV"} – username not available</li>
 *   <li>{@code "ENV"} – e-mail not available</li>
 *   <li>{@code "KO"}  – unexpected error</li>
 * </ul>
 */
public class RegisterResult {

    private final String response;

    public RegisterResult(String response) {
        this.response = response;
    }

    /** @return registration outcome code */
    public String getResponse() { return response; }

    /** @return {@code true} if the user was created successfully */
    public boolean isSuccess() { return "OK".equals(response); }
}
