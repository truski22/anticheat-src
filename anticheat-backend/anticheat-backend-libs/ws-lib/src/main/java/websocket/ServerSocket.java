package websocket;

import org.glassfish.tyrus.server.Server;

import javax.websocket.DeploymentException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Class to create the server for websocket
 */
public class ServerSocket {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ServerSocket.class);
    private static final CountDownLatch latch = new CountDownLatch(1);
    private Server server;
    public ServerSocket(String hostName, int port, String contextPath, Map<String, Object> properties, Class<?>... configuration){
        server = new Server(hostName, port, contextPath, properties, configuration );
    }
    public void start(){
        try {
            if(server!=null){
                server.start();
                log.info("Server started");
            }
        } catch (DeploymentException e) {
            log.error("Failed to start WebSocket server: {}", e.getMessage(), e);
            server.stop();
        }
    }
    public void stop(){
        if(server!=null) {
            server.stop();
        }
    }
}