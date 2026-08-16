package websocket;


import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseWebSocket {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BaseWebSocket.class);
    // Map to keep sessions by topic
    private static Map<String, javax.websocket.Session> topicSessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(javax.websocket.Session session, @PathParam("topic") String topic) {
        topicSessions.put(topic, session);
        log.info("New connection on topic: {}", topic);
    }
    @OnClose
    public void onClose(@PathParam("topic") String topic, javax.websocket.Session session) {
        topicSessions.remove(topic);
        log.info("Connection closed for topic: {}", topic);
    }
    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("WebSocket error: {}", throwable.getMessage());
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
    @OnMessage
    protected abstract void onMessage(String message, @PathParam("topic") String topic, Session session) throws IOException;
}
