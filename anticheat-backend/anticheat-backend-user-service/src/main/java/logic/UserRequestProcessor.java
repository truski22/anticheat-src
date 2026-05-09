package logic;

import config.UserServiceConfig;
import handler.*;
import mqtt.MqttInterface;
import mqtt.MqttMessageRouter;
import mqtt.exception.MqttException;
import service.EmailNotificationService;
import service.UserAccountService;
import utils.Database;

import java.util.List;

public class UserRequestProcessor {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserRequestProcessor.class);

    private final MqttInterface mqttInterface;
    private final MqttMessageRouter router;
    private final Database database;

    public UserRequestProcessor(String path) {
        try {
            mqttInterface = new MqttInterface(path);
            database      = new Database();

            UserServiceConfig        config       = UserServiceConfig.fromEnvironment();
            UserAccountService       accountService = new UserAccountService(database);
            EmailNotificationService emailService = new EmailNotificationService(config);

            router = mqttInterface.createRouter(List.of(
                new LoginRequestHandler(accountService, mqttInterface),
                new RegisterRequestHandler(accountService, mqttInterface),
                new UserInfoRequestHandler(accountService, mqttInterface),
                new ChangePasswordRequestHandler(accountService, mqttInterface),
                new ChangePasswordEmailRequestHandler(accountService, mqttInterface),
                new ForgotPasswordEmailHandler(accountService, emailService, mqttInterface)
            ));
            router.start();
        } catch (MqttException e) {
            log.error("Failed to initialize UserRequestProcessor: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to initialize UserRequestProcessor", e);
        }
    }

    public void stop() {
        router.stopRouter();
        if (mqttInterface != null) mqttInterface.stopInterface();
        if (database      != null) database.close();
    }
}
