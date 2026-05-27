package protocol;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChessMessage(
    MessageType type,
    String messageId,
    Object payload
) {
    public ChessMessage(MessageType type, Object payload) {
        this(type, UUID.randomUUID().toString(), payload);
    }

    public static ChessMessage error(String code, String message) {
        return new ChessMessage(MessageType.ERROR, new ErrorPayload(code, message));
    }

    public static ChessMessage error(String code, String message, String correlationId) {
        return new ChessMessage(MessageType.ERROR, correlationId, new ErrorPayload(code, message));
    }
}
