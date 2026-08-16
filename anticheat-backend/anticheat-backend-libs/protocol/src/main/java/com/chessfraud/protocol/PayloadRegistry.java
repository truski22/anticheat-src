package com.chessfraud.protocol;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chessfraud.protocol.exception.InvalidMessageException;
import com.chessfraud.protocol.payload.*;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * Resolves the polymorphic {@code payload} of a {@link ChessMessage} based on its
 * {@link MessageType}, and (de)serializes the envelope to/from wire JSON.
 *
 * <p>This is the single trust boundary for untrusted client input coming off the
 * WebSocket: every inbound frame is parsed here before any business logic sees it.
 * Hardening applied on top of Jackson defaults:
 * <ul>
 *   <li>{@link #MAX_MESSAGE_BYTES} rejects oversized frames before parsing starts,
 *       preventing large-payload memory exhaustion.</li>
 *   <li>{@link StreamReadConstraints} caps nesting depth and string/number length,
 *       preventing stack-overflow and allocation-based denial of service from a
 *       single crafted frame (e.g. deeply nested arrays, multi-megabyte strings).</li>
 *   <li>No polymorphic type handling ({@code activateDefaultTyping}) is enabled on
 *       the mapper, so this class is not exposed to Jackson deserialization-gadget
 *       attacks; payload classes are resolved from a fixed, compile-time registry.</li>
 * </ul>
 */
public final class PayloadRegistry {

    /** Hard cap on inbound message size. Chess move lists are small; 256 KiB is generous. */
    public static final int MAX_MESSAGE_BYTES = 256 * 1024;

    private static final int MAX_NESTING_DEPTH = 64;
    private static final int MAX_STRING_LENGTH_CHARS = MAX_MESSAGE_BYTES;

    private static final ObjectMapper MAPPER = buildMapper();
    private static final Map<MessageType, Class<?>> REGISTRY = new EnumMap<>(MessageType.class);

    static {
        // Client -> Gateway
        REGISTRY.put(MessageType.LOGIN, LoginPayload.class);
        REGISTRY.put(MessageType.REGISTER, RegisterPayload.class);
        REGISTRY.put(MessageType.ANALYZE_GAME, AnalyzeGamePayload.class);
        REGISTRY.put(MessageType.SAVE_GAME, SaveGamePayload.class);
        REGISTRY.put(MessageType.CHANGE_PASSWORD, ChangePasswordPayload.class);
        REGISTRY.put(MessageType.CHANGE_PASSWORD_EMAIL, ChangePasswordEmailPayload.class);
        REGISTRY.put(MessageType.SEND_EMAIL_CHANGE_PASSWORD, SendEmailChangePasswordPayload.class);
        // USER_INFO_REQUEST and GAMES_REQUEST have no payload

        // Gateway -> Client
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

    private static ObjectMapper buildMapper() {
        StreamReadConstraints constraints = StreamReadConstraints.builder()
            .maxNestingDepth(MAX_NESTING_DEPTH)
            .maxStringLength(MAX_STRING_LENGTH_CHARS)
            .build();
        JsonFactory factory = JsonFactory.builder()
            .streamReadConstraints(constraints)
            .build();
        return new ObjectMapper(factory);
    }

    /**
     * Deserialize a raw JSON string into a ChessMessage with a typed payload.
     * @throws InvalidMessageException if the message is malformed, oversized, or fails payload validation
     */
    public static ChessMessage deserialize(String json) throws InvalidMessageException {
        if (json == null) {
            throw new InvalidMessageException("MALFORMED_JSON", "Message must not be null");
        }
        if (json.getBytes(StandardCharsets.UTF_8).length > MAX_MESSAGE_BYTES) {
            throw new InvalidMessageException("MESSAGE_TOO_LARGE",
                "Message exceeds the maximum allowed size of " + MAX_MESSAGE_BYTES + " bytes");
        }

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
            throw new IllegalStateException("Failed to serialize message", e);
        }
    }
}
