package websocket;
import org.glassfish.tyrus.server.Server;
public class Main {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
        Server server = new Server("localhost", 8080, "/", null, WebSocketServer.class );
        try {
            server.start();
            log.info("Servidor conectado");
            System.in.read();
        } catch (Exception e) {
            log.error("Failed to start WebSocket server: {}", e.getMessage(), e);
            server.stop();
        }
    }
}
