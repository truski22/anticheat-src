package websocket;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DEMO ONLY - NOT FOR PRODUCTION USE.
 * This class is a local development example showing basic WebSocket session management.
 * The production implementation is gateway/src/main/java/websocket/WebSocket.java,
 * which enforces JWT authentication on every connection.
 *
 * DO NOT deploy this class or reference it outside of local testing.
 */
@ServerEndpoint("/ws/{topic}")
public class WebSocketServer {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WebSocketServer.class);

    private static Map<String, Session> topicSessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("topic") String topic) {
        topicSessions.put(topic, session);
        log.info("[DEMO] New connection on topic: {}", topic);
    }

    @OnMessage
    public void onMessage(String message, @PathParam("topic") String topic, Session session) {
        log.info("[DEMO] Message received on topic [{}]: {}", topic, message);
    }

    @OnClose
    public void onClose(@PathParam("topic") String topic, Session session) {
        topicSessions.remove(topic);
        log.info("[DEMO] Connection closed on topic: {}", topic);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("[DEMO] WebSocket error: {}", throwable.getMessage());
    }

    public static void sendToTopic(String topic, String message) {
        Session session = topicSessions.get(topic);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                log.error("Failed to send message to topic {}: {}", topic, e.getMessage(), e);
            }
        }
    }
}
