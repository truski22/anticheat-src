package mqtt;

/**
 * Contract for handling a specific MQTT message type.
 * <p>
 * Implement this interface for each message type your service needs to process.
 * Register all implementations with an {@link MqttMessageRouter} to replace
 * manual {@code if-else} dispatch chains.
 *
 * <pre>{@code
 * public class LoginRequestHandler implements MqttPayloadHandler {
 *
 *     @Override
 *     public String getHandledMessageType() { return "LoginRequest"; }
 *
 *     @Override
 *     public void handle(String jsonPayload) {
 *         // process the message
 *     }
 * }
 * }</pre>
 */
public interface MqttPayloadHandler {

    /**
     * Returns the value of the {@code messageType} JSON field this handler processes.
     *
     * @return message type string (e.g. {@code "LoginRequest"})
     */
    String getHandledMessageType();

    /**
     * Processes an incoming MQTT message payload.
     *
     * @param jsonPayload raw JSON string received from the broker
     */
    void handle(String jsonPayload);
}
