package handler;

import mqtt.MqttPayloadHandler;

import mqtt.MqttInterface;
import mqtt.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.UserAccountService;
import service.UserInfoResult;
import utils.JsonUtils;

/**
 * Handles {@code UserInfoRequest} MQTT messages.
 * Fetches profile statistics from {@link UserAccountService} and publishes a
 * {@link UserInfo} response.
 */
public class UserInfoRequestHandler implements MqttPayloadHandler {

    private static final Logger log = LoggerFactory.getLogger(UserInfoRequestHandler.class);

    private final UserAccountService accountService;
    private final MqttInterface mqttInterface;

    /**
     * @param accountService service that retrieves user profile data
     * @param mqttInterface  used to publish the response message
     */
    public UserInfoRequestHandler(UserAccountService accountService, MqttInterface mqttInterface) {
        this.accountService = accountService;
        this.mqttInterface  = mqttInterface;
    }

    /**
     * Parses the incoming MQTT JSON payload, retrieves user statistics, and publishes a
     * {@link UserInfo} message.
     * <p>Does nothing if the required fields ({@code user}, {@code idMessage}) are missing
     * or the user does not exist in the database.</p>
     *
     * @param mqttMessage raw JSON string received from the MQTT broker
     */
    @Override
    public String getHandledMessageType() { return "UserInfoRequest"; }

    @Override

    public void handle(String mqttMessage) {
        String userName  = JsonUtils.extractField(mqttMessage, "user");
        String idMessage = JsonUtils.extractField(mqttMessage, "idMessage");

        if (userName == null || idMessage == null) {
            log.warn("[UserInfoRequestHandler] Missing required fields in UserInfoRequest message");
            return;
        }

        UserInfoResult result = accountService.getUserInfo(userName);
        if (result == null) {
            log.warn("[UserInfoRequestHandler] User '{}' not found", userName);
            return;
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setMessageType("UserInfo");
        userInfo.setIdMessage(idMessage);
        userInfo.setUser(userName);
        userInfo.setEmail(result.getEmail());
        userInfo.setTotalGames(result.getTotalGames());
        userInfo.setCheatGames(result.getCheatedGames());
        userInfo.setLegalGames(result.getFairGames());

        mqttInterface.publishMessage(userInfo);
    }
}


