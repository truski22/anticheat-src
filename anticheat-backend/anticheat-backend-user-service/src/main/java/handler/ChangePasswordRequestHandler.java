package handler;

import mqtt.MqttPayloadHandler;

import mqtt.ChangePasswordResponse;
import mqtt.MqttInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.UserAccountService;
import utils.JsonUtils;

/**
 * Handles {@code ChangePassword} MQTT messages (password change by username).
 * Delegates the update to {@link UserAccountService} and publishes a
 * {@link ChangePasswordResponse}.
 */
public class ChangePasswordRequestHandler implements MqttPayloadHandler {

    private static final Logger log = LoggerFactory.getLogger(ChangePasswordRequestHandler.class);

    private final UserAccountService accountService;
    private final MqttInterface mqttInterface;

    /**
     * @param accountService service that performs the password update
     * @param mqttInterface  used to publish the response message
     */
    public ChangePasswordRequestHandler(UserAccountService accountService, MqttInterface mqttInterface) {
        this.accountService = accountService;
        this.mqttInterface  = mqttInterface;
    }

    /**
     * Parses the incoming MQTT JSON payload, updates the user's password, and publishes a
     * {@link ChangePasswordResponse}.
     * <p>Does nothing if any required field ({@code user}, {@code password},
     * {@code idMessage}) is missing.</p>
     *
     * @param mqttMessage raw JSON string received from the MQTT broker
     */
    @Override
    public String getHandledMessageType() { return "ChangePassword"; }

    @Override

    public void handle(String mqttMessage) {
        String user      = JsonUtils.extractField(mqttMessage, "user");
        String password  = JsonUtils.extractField(mqttMessage, "password");
        String idMessage = JsonUtils.extractField(mqttMessage, "idMessage");

        if (user == null || password == null || idMessage == null) {
            log.warn("[ChangePasswordRequestHandler] Missing required fields in ChangePassword message");
            return;
        }

        boolean changed = accountService.changePassword(user, password);

        ChangePasswordResponse response = new ChangePasswordResponse();
        response.setUser(user);
        response.setChanged(changed);
        response.setMessageType("ChangePasswordResponse");

        mqttInterface.publishMessage(response);
    }
}


