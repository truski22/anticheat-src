package handler;

import mqtt.MqttPayloadHandler;

import mqtt.ChangePasswordEmailResponse;
import mqtt.MqttInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.EmailNotificationService;
import service.UserAccountService;
import utils.JsonUtils;

import java.security.SecureRandom;

/**
 * Handles {@code ChangePasswordSendEmail} MQTT messages (forgot-password flow).
 * <ol>
 *   <li>Checks whether the requested e-mail is registered via {@link UserAccountService}.</li>
 *   <li>If found, generates a secure six-digit verification code and sends it via
 *       {@link EmailNotificationService}.</li>
 *   <li>Publishes a {@link ChangePasswordEmailResponse} with the code (or {@code null}
 *       if the e-mail is not registered or sending failed).</li>
 * </ol>
 */
public class ForgotPasswordEmailHandler implements MqttPayloadHandler {

    private static final Logger log = LoggerFactory.getLogger(ForgotPasswordEmailHandler.class);

    private final UserAccountService      accountService;
    private final EmailNotificationService emailService;
    private final MqttInterface            mqttInterface;

    /**
     * @param accountService service used to check whether the e-mail is registered
     * @param emailService   service that sends the verification code e-mail
     * @param mqttInterface  used to publish the response message
     */
    public ForgotPasswordEmailHandler(
            UserAccountService accountService,
            EmailNotificationService emailService,
            MqttInterface mqttInterface) {
        this.accountService = accountService;
        this.emailService   = emailService;
        this.mqttInterface  = mqttInterface;
    }

    /**
     * Parses the incoming MQTT JSON payload, conditionally sends a verification e-mail,
     * and publishes a {@link ChangePasswordEmailResponse}.
     * <p>Does nothing if any required field ({@code email}, {@code idMessage}) is missing.</p>
     *
     * @param mqttMessage raw JSON string received from the MQTT broker
     */
    @Override
    public String getHandledMessageType() { return "ChangePasswordSendEmail"; }

    @Override

    public void handle(String mqttMessage) {
        String email     = JsonUtils.extractField(mqttMessage, "email");
        String idMessage = JsonUtils.extractField(mqttMessage, "idMessage");

        if (email == null || idMessage == null) {
            log.warn("[ForgotPasswordEmailHandler] Missing required fields in ChangePasswordSendEmail message");
            return;
        }

        ChangePasswordEmailResponse response = new ChangePasswordEmailResponse();
        response.setEmail(email);
        response.setMessageType("ChangePasswordEmailResponse");

        if (accountService.emailExists(email)) {
            String code   = generateRandomCode();
            boolean sent  = emailService.sendVerificationCode(email, code);
            response.setCode(sent ? code : null);
        } else {
            log.info("[ForgotPasswordEmailHandler] E-mail '{}' is not registered", email);
            response.setCode(null);
        }

        mqttInterface.publishMessage(response);
    }

    /** Generates a cryptographically secure six-digit numeric code. */
    private String generateRandomCode() {
        SecureRandom secureRandom = new SecureRandom();
        int code = 100_000 + secureRandom.nextInt(900_000);
        return String.valueOf(code);
    }
}


