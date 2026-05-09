package handler;

import auth.AuthController;
import mqtt.MqttPayloadHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protocol.ChessMessage;
import protocol.MessageType;
import protocol.payload.ResponsePayload;
import utils.JsonUtils;
import websocket.WebSocket;

/** Routes a {@code LoginResponse} MQTT message to the waiting REST auth request or WebSocket session. */
public class LoginResponseHandler implements MqttPayloadHandler {

    private static final Logger log = LoggerFactory.getLogger(LoginResponseHandler.class);

    private final WebSocket webSocket;
    private final AuthController authController;

    public LoginResponseHandler(WebSocket webSocket, AuthController authController) {
        this.webSocket      = webSocket;
        this.authController = authController;
    }

    @Override
    public String getHandledMessageType() { return "LoginResponse"; }

    @Override
    public void handle(String jsonPayload) {
        String user      = JsonUtils.extractField(jsonPayload, "user");
        String response  = JsonUtils.extractField(jsonPayload, "response");
        String idMessage = JsonUtils.extractField(jsonPayload, "idMessage");
        boolean success  = "OK".equalsIgnoreCase(response);
        ResponsePayload payload = new ResponsePayload(success, response);

        if (idMessage != null && idMessage.startsWith("login:")) {
            authController.completeAuthRequest(idMessage, payload);
        } else {
            webSocket.sendToUser(user, new ChessMessage(MessageType.LOGIN_RESPONSE, payload));
        }
    }
}
