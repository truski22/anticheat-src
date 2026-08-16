package com.chessfraud.ws;

import org.glassfish.tyrus.server.Server;

import javax.websocket.DeploymentException;
import java.util.Map;

/**
 * Thin lifecycle wrapper around a standalone Tyrus WebSocket server.
 *
 * <p><b>Legacy / transitional:</b> this class exists only to bootstrap the gateway's
 * current {@code javax.websocket} endpoint. Once the gateway migrates onto
 * {@code spring-boot-starter-websocket} (see the anticheat-backend-libs README),
 * Spring Boot's embedded server owns the WebSocket lifecycle and this class -
 * along with the {@code ws-lib} module - can be retired entirely.
 *
 * <p>Deliberately named to avoid colliding with {@link java.net.ServerSocket}: the
 * previous name ({@code websocket.ServerSocket}) shadowed the JDK class of the same
 * simple name, which is a common source of import confusion.
 */
public final class WebSocketServerBootstrap {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WebSocketServerBootstrap.class);

    private final Server server;

    public WebSocketServerBootstrap(String hostName, int port, String contextPath,
                                     Map<String, Object> properties, Class<?>... endpointClasses) {
        this.server = new Server(hostName, port, contextPath, properties, endpointClasses);
    }

    public void start() {
        try {
            server.start();
            log.info("[WS] WebSocket server started");
        } catch (DeploymentException e) {
            log.error("[WS] Failed to start WebSocket server: {}", e.getMessage(), e);
            server.stop();
            throw new IllegalStateException("Failed to start WebSocket server", e);
        }
    }

    public void stop() {
        server.stop();
    }
}
