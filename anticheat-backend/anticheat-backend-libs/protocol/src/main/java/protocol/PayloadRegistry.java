package protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import protocol.payload.*;

import java.util.EnumMap;
import java.util.Map;

public final class PayloadRegistry {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<MessageType, Class<?>> REGISTRY = new EnumMap<>(MessageType.class);

    static {
        // Client → Gateway
        REGISTRY.put(MessageType.LOGIN, LoginPayload.class);
        REGISTRY.put(MessageType.REGISTER, RegisterPayload.class);
        REGISTRY.put(MessageType.ANALYZE_GAME, AnalyzeGamePayload.class);
        REGISTRY.put(MessageType.SAVE_GAME, SaveGamePayload.class);
        REGISTRY.put(MessageType.CHANGE_PASSWORD, ChangePasswordPayload.class);
        REGISTRY.put(MessageType.CHANGE_PASSWORD_EMAIL, ChangePasswordEmailPayload.class);
        REGISTRY.put(MessageType.SEND_EMAIL_CHANGE_PASSWORD, SendEmailChangePasswordPayload.class);
        // USER_INFO_REQUEST and GAMES_REQUEST have no payload

        // Gateway → Client
        REGISTRY.put(MessageType.LOGIN_RESPONSE, ResponsePayload.class);
        REGISTRY.put(MessageType.REGISTER_RESPONSE, ResponsePayload.class);
        REGISTRY.put(MessageType.SAVE_GAME_RESPONSE, ResponsePayload.class);
        REGISTRY.put(MessageType.CHANGE_PASSWORD_RESPONSE, ResponsePayload.class);
        REGISTRY.put(MessageType.USER_INFO, UserInfoPayload.class);
        REGISTRY.put(MessageType.GAMES, GamesPayload.class);
        REGISTRY.put(MessageType.ANALYZE_RESULT, AnalyzeResultPayload.class);
        REGISTRY.put(MessageType.CHANGE_PASSWORD_EMAIL_RESPONSE, ChangePasswordEmailResponsePayload.class);
        REGISTRY.put(MessageType.ERROR, ErrorPayload.class);

        // Future
        REGISTRY.put(MessageType.MOVE, MovePayload.class);
        REGISTRY.put(MessageType.FRAUD_ALERT, FraudAlertPayload.class);
    }

    private PayloadRegistry() {}

    /**
     * Deserialize a raw JSON string into a ChessMessage with a typed payload.
     * @throws InvalidMessageException if the message is malformed
     */
    public static ChessMessage deserialize(String json) throws InvalidMessageException {
        try {
            JsonNode root = MAPPER.readTree(json);

            if (!root.has("type")) {
                throw new InvalidMessageException("MISSING_TYPE", "Message must include a 'type' field");
            }

            String typeStr = root.get("type").asText();
            MessageType type;
            try {
                type = MessageType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                throw new InvalidMessageException("UNKNOWN_TYPE", "Unknown message type: " + typeStr);
            }

            String messageId = root.has("messageId") ? root.get("messageId").asText() : null;

            Object payload = null;
            if (root.has("payload") && !root.get("payload").isNull()) {
                Class<?> payloadClass = REGISTRY.get(type);
                if (payloadClass != null) {
                    try {
                        payload = MAPPER.treeToValue(root.get("payload"), payloadClass);
                    } catch (Exception e) {
                        throw new InvalidMessageException("INVALID_PAYLOAD",
                            "Invalid payload for type " + type + ": " + e.getMessage());
                    }
                }
            } else if (REGISTRY.containsKey(type)) {
                throw new InvalidMessageException("MISSING_PAYLOAD",
                    "Message type " + type + " requires a payload");
            }

            return new ChessMessage(type, messageId, payload);
        } catch (InvalidMessageException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidMessageException("MALFORMED_JSON", "Could not parse message: " + e.getMessage());
        }
    }

    /**
     * Serialize a ChessMessage to JSON string.
     */
    public static String serialize(ChessMessage message) {
        try {
            return MAPPER.writeValueAsString(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize message", e);
        }
    }
}
