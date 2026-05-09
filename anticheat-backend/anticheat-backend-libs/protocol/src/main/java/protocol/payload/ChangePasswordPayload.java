package protocol.payload;

public record ChangePasswordPayload(String password) {
    public ChangePasswordPayload {
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password is required");
    }
}
