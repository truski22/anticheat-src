package handler;

import auth.AuthController;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mqtt.MqttPayloadHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles {@code ChangePasswordEmailResponse} from user-service.
 * Extracts the reset code and stores it in AuthController for later verification
 * when the user submits POST /auth/reset-password.
 */
public class ChangePasswordEmailResponseHandler implements MqttPayloadHandler {

    private static final Logger log = LoggerFactory.getLogger(ChangePasswordEmailResponseHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final AuthController authController;

    public ChangePasswordEmailResponseHandler(AuthController authController) {
        this.authController = authController;
    }

    @Override
    public String getHandledMessageType() { return "ChangePasswordEmailResponse"; }

    @Override
    public void handle(String jsonPayload) {
        try {
            JsonNode node = MAPPER.readTree(jsonPayload);
            String email = node.path("email").asText(null);
            String code = node.path("code").asText(null);

            if (email != null && code != null) {
                authController.storeResetCode(email, code);
                log.info("[MQTT] Reset code stored for email '{}'", email);
            } else {
                log.warn("[MQTT] ChangePasswordEmailResponse missing email or code");
            }
        } catch (Exception e) {
            log.error("[MQTT] Failed to parse ChangePasswordEmailResponse", e);
        }
    }
}
