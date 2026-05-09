package handler;

import mqtt.MqttPayloadHandler;

import mqtt.MqttInterface;
import mqtt.RegisterResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.RegisterResult;
import service.UserAccountService;
import utils.JsonUtils;

/**
 * Handles {@code RegisterRequest} MQTT messages.
 * Delegates account creation to {@link UserAccountService} and publishes a
 * {@link RegisterResponse} with the outcome code ({@code "OK"}, {@code "UNV"}, or {@code "ENV"}).
 */
public class RegisterRequestHandler implements MqttPayloadHandler {

    private static final Logger log = LoggerFactory.getLogger(RegisterRequestHandler.class);

    private final UserAccountService accountService;
    private final MqttInterface mqttInterface;

    /**
     * @param accountService service that creates the new account
     * @param mqttInterface  used to publish the response message
     */
    public RegisterRequestHandler(UserAccountService accountService, MqttInterface mqttInterface) {
        this.accountService = accountService;
        this.mqttInterface  = mqttInterface;
    }

    /**
     * Parses the incoming MQTT JSON payload, attempts user registration, and publishes a
     * {@link RegisterResponse}.
     * <p>Does nothing if any required field ({@code user}, {@code email},
     * {@code password}) is missing.</p>
     *
     * @param mqttMessage raw JSON string received from the MQTT broker
     */
    @Override
    public String getHandledMessageType() { return "RegisterRequest"; }

    @Override

    public void handle(String mqttMessage) {
        String user      = JsonUtils.extractField(mqttMessage, "user");
        String email     = JsonUtils.extractField(mqttMessage, "email");
        String password  = JsonUtils.extractField(mqttMessage, "password");
        String idMessage = JsonUtils.extractField(mqttMessage, "idMessage");

        if (user == null || email == null || password == null) {
            log.warn("[RegisterRequestHandler] Missing required fields in RegisterRequest message");
            return;
        }

        RegisterResult result = accountService.register(user, email, password);

        RegisterResponse response = new RegisterResponse();
        response.setIdMessage(idMessage);
        response.setMessageType("RegisterResponse");
        response.setUser(user);
        response.setResponse(result.getResponse());

        mqttInterface.publishMessage(response);
    }
}


