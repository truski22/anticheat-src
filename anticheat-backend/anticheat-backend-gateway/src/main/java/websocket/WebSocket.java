package websocket;

import auth.JwtService;
import com.auth0.jwt.exceptions.JWTVerificationException;
import logic.GatewayMessageRouter;
import protocol.ChessMessage;
import protocol.InvalidMessageException;
import protocol.PayloadRegistry;
import ratelimit.RateLimiter;

import javax.websocket.*;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/ws", configurator = WebSocket.JwtConfigurator.class)
public class WebSocket {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WebSocket.class);
    private static final Map<String, Session> userSessions = new ConcurrentHashMap<>();
    private static final RateLimiter rateLimiter = new RateLimiter(10);
    private static final String USER_KEY = "authenticated.user";

    private static GatewayMessageRouter logic;
    private static JwtService jwtService;

    public static void setLogic(GatewayMessageRouter logicInstance) {
        logic = logicInstance;
    }

    public static void setJwtService(JwtService service) {
        jwtService = service;
    }

    @OnOpen
    public void onOpen(Session session) {
        // Tyrus 1.x bug: config.getUserProperties() in modifyHandshake does not
        // propagate to session.getUserProperties(). Validate JWT directly here.
        String user = authenticateSession(session);
        if (user == null) {
            log.error("[WS] Unauthenticated connection attempt, closing");
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY,
                    "Authentication required. Connect with ?token=JWT"));
            } catch (IOException e) {
                log.error("[WS] Failed to close unauthenticated session");
            }
            return;
        }
        session.getUserProperties().put(USER_KEY, user);
        userSessions.put(user, session);
        log.info("[WS] Authenticated connection: {}", user);
    }

    private String authenticateSession(Session session) {
        // First check if modifyHandshake already set the user (works in newer Tyrus)
        String user = (String) session.getUserProperties().get(USER_KEY);
        if (user != null) return user;

        // Fallback: extract and validate token from query parameters
        java.util.List<String> tokens = session.getRequestParameterMap().get("token");
        if (tokens == null || tokens.isEmpty() || jwtService == null) return null;
        try {
            return jwtService.validateToken(tokens.get(0));
        } catch (JWTVerificationException e) {
            log.error("[WS] JWT validation failed: {}", e.getMessage());
            return null;
        }
    }

    @OnClose
    public void onClose(Session session) {
        String user = (String) session.getUserProperties().get(USER_KEY);
        if (user != null) {
            userSessions.remove(user);
            rateLimiter.removeSession(user);
            log.info("[WS] Connection closed: {}", user);
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("[WS] Error: {}", throwable.getMessage());
    }

    @OnMessage
    public void onMessage(String rawMessage, Session session) {
        String user = (String) session.getUserProperties().get(USER_KEY);

        // Rate limiting
        if (!rateLimiter.tryAcquire(user)) {
            log.error("[WS] Rate limit exceeded for {}", user);
            sendMessage(session, ChessMessage.error("RATE_LIMIT_EXCEEDED",
                "Too many messages. Max 10 per second."));
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY,
                    "Rate limit exceeded"));
            } catch (IOException e) {
                log.error("[WS] Failed to close session: {}", e.getMessage(), e);
            }
            return;
        }

        try {
            ChessMessage message = PayloadRegistry.deserialize(rawMessage);
            logic.processWebSocketMessage(message, user);
        } catch (InvalidMessageException e) {
            log.error("[WS] Invalid message from {}: {}", user, e.getMessage());
            sendMessage(session, ChessMessage.error(e.getCode(), e.getMessage()));
        }
    }

    public void sendToUser(String username, ChessMessage message) {
        Session session = userSessions.get(username);
        if (session != null && session.isOpen()) {
            sendMessage(session, message);
        }
    }

    private void sendMessage(Session session, ChessMessage message) {
        try {
            session.getBasicRemote().sendText(PayloadRegistry.serialize(message));
        } catch (IOException e) {
            log.error("[WS] Failed to send message: {}", e.getMessage(), e);
        }
    }

    // ── JWT Handshake Configurator ────────────────────────────────────────

    public static class JwtConfigurator extends ServerEndpointConfig.Configurator {
        @Override
        public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request,
                                     javax.websocket.HandshakeResponse response) {
            String token = extractToken(request);
            if (token != null && jwtService != null) {
                try {
                    String username = jwtService.validateToken(token);
                    config.getUserProperties().put(USER_KEY, username);
                } catch (JWTVerificationException e) {
                    log.error("[WS] JWT validation failed: {}", e.getMessage());
                }
            }
        }

        @Override
        public boolean checkOrigin(String originHeaderValue) {
            String allowedOriginsEnv = System.getenv("ALLOWED_ORIGINS");
            if (allowedOriginsEnv == null || allowedOriginsEnv.isBlank()) {
                log.error("[WS] WARNING: ALLOWED_ORIGINS not set, rejecting all cross-origin connections.");
                return originHeaderValue == null || originHeaderValue.isBlank();
            }
            if (originHeaderValue == null || originHeaderValue.isBlank()) {
                return true;
            }
            for (String allowed : allowedOriginsEnv.split(",")) {
                if (allowed.trim().equalsIgnoreCase(originHeaderValue.trim())) {
                    return true;
                }
            }
            log.error("[WS] Rejected connection from unauthorized origin: {}", originHeaderValue);
            return false;
        }

        private String extractToken(HandshakeRequest request) {
            java.util.List<String> values = request.getParameterMap().get("token");
            if (values != null && !values.isEmpty()) {
                return values.get(0);
            }
            return null;
        }
    }
}
