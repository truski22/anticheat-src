package service;

/**
 * Represents the outcome of a login attempt.
 * {@code response} is {@code "OK"} on success and {@code "KO"} on failure.
 */
public class LoginResult {

    private final String response;

    public LoginResult(String response) {
        this.response = response;
    }

    /** @return {@code "OK"} if credentials are valid, {@code "KO"} otherwise */
    public String getResponse() { return response; }

    /** @return {@code true} if login succeeded */
    public boolean isSuccess() { return "OK".equals(response); }
}
