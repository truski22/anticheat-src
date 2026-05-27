package protocol;

public class InvalidMessageException extends Exception {
    private final String code;

    public InvalidMessageException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
