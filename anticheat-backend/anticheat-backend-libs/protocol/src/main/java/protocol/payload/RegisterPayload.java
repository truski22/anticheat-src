package protocol.payload;

public record RegisterPayload(String email, String password) {
    public RegisterPayload {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password is required");
    }
}
