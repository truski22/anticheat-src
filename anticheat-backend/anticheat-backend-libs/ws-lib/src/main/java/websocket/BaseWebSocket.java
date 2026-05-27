package websocket;


import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseWebSocket {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BaseWebSocket.class);
    // Mapa para mantener las sesiones por tópico
    private static Map<String, javax.websocket.Session> topicSessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(javax.websocket.Session session, @PathParam("topic") String topic) {
        topicSessions.put(topic, session);
        log.info("Nueva conexión en el tópico: {}", topic);
    }
    @OnClose
    public void onClose(@PathParam("topic") String topic, javax.websocket.Session session) {
        topicSessions.remove(topic);
        log.info("Conexión cerrada para el tópico: {}", topic);
    }
    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("Error en WebSocket: {}", throwable.getMessage());
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
