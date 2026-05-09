package handler;

import mqtt.MqttPayloadHandler;

import mqtt.MqttInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.UserAccountService;
import utils.JsonUtils;

/**
 * Handles {@code ChangePasswordEmail} MQTT messages (password update identified by e-mail).
 * Delegates the update to {@link UserAccountService}.
 * No MQTT response is published; this is a fire-and-forget update triggered after the
 * verification code has already been confirmed by the client.
 */
public class ChangePasswordEmailRequestHandler implements MqttPayloadHandler {

    private static final Logger log = LoggerFactory.getLogger(ChangePasswordEmailRequestHandler.class);

    private final UserAccountService accountService;
    private final MqttInterface mqttInterface;

    /**
     * @param accountService service that performs the password update by e-mail
     * @param mqttInterface  retained for consistency; not used to publish a response
     */
    public ChangePasswordEmailRequestHandler(UserAccountService accountService, MqttInterface mqttInterface) {
        this.accountService = accountService;
        this.mqttInterface  = mqttInterface;
    }

    /**
     * Parses the incoming MQTT JSON payload and updates the password for the account
     * associated with the provided e-mail address.
     * <p>Does nothing if any required field ({@code email}, {@code password}) is missing.</p>
     *
     * @param mqttMessage raw JSON string received from the MQTT broker
     */
    @Override
    public String getHandledMessageType() { return "ChangePasswordEmail"; }

    @Override

    public void handle(String mqttMessage) {
        String email    = JsonUtils.extractField(mqttMessage, "email");
        String password = JsonUtils.extractField(mqttMessage, "password");

        if (email == null || password == null) {
            log.warn("[ChangePasswordEmailRequestHandler] Missing required fields in ChangePasswordEmail message");
            return;
        }

        boolean updated = accountService.changePasswordByEmail(email, password);
        if (!updated) {
            log.warn("[ChangePasswordEmailRequestHandler] Password update by email failed for '{}'", email);
        }
    }
}


