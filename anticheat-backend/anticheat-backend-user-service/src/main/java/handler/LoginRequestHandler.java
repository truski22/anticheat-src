package handler;

import mqtt.MqttPayloadHandler;

import mqtt.LoginResponse;
import mqtt.MqttInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.LoginResult;
import service.UserAccountService;
import utils.JsonUtils;

/**
 * Handles {@code LoginRequest} MQTT messages.
 * Delegates credential validation to {@link UserAccountService} and publishes
 * a {@link LoginResponse} with the result code ({@code "OK"} or {@code "KO"}).
 */
public class LoginRequestHandler implements MqttPayloadHandler {

    private static final Logger log = LoggerFactory.getLogger(LoginRequestHandler.class);

    private final UserAccountService accountService;
    private final MqttInterface mqttInterface;

    /**
     * @param accountService service that performs the credential check
     * @param mqttInterface  used to publish the response message
     */
    public LoginRequestHandler(UserAccountService accountService, MqttInterface mqttInterface) {
        this.accountService = accountService;
        this.mqttInterface  = mqttInterface;
    }

    /**
     * Parses the incoming MQTT JSON payload, validates credentials, and publishes a
     * {@link LoginResponse}.
     * <p>Does nothing if any required field ({@code user}, {@code password},
     * {@code idMessage}) is missing.</p>
     *
     * @param mqttMessage raw JSON string received from the MQTT broker
     */
    @Override
    public String getHandledMessageType() { return "LoginRequest"; }

    @Override

    public void handle(String mqttMessage) {
        String user      = JsonUtils.extractField(mqttMessage, "user");
        String password  = JsonUtils.extractField(mqttMessage, "password");
        String idMessage = JsonUtils.extractField(mqttMessage, "idMessage");

        if (user == null || password == null || idMessage == null) {
            log.warn("[LoginRequestHandler] Missing required fields in LoginRequest message");
            return;
        }

        LoginResult result = accountService.login(user, password);

        LoginResponse response = new LoginResponse();
        response.setIdMessage(idMessage);
        response.setMessageType("LoginResponse");
        response.setUser(user);
        response.setResponse(result.getResponse());

        mqttInterface.publishMessage(response);
    }
}


