package com.chessfraud.gateway.websocket.handler;

import com.chessfraud.gateway.ratelimit.RateLimiter;
import com.chessfraud.gateway.websocket.service.GatewayMessageRouter;
import com.chessfraud.protocol.ChessMessage;
import com.chessfraud.protocol.PayloadRegistry;
import com.chessfraud.protocol.exception.InvalidMessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ChessWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(ChessWebSocketHandler.class);
    public static final String USER_ATTR = "authenticatedUser";

    private final GatewayMessageRouter router;
    private final RateLimiter rateLimiter = new RateLimiter(10);
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public ChessWebSocketHandler(GatewayMessageRouter router) {
        this.router = router;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("[WS] Authenticated connection: {}", user(session));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String user = user(session);

        if (!rateLimiter.tryAcquire(user)) {
            log.error("[WS] Rate limit exceeded for {}", user);
            sendMessage(session, ChessMessage.error("RATE_LIMIT_EXCEEDED",
                "Too many messages. Max 10 per second."));
            closeQuietly(session, CloseStatus.POLICY_VIOLATION.withReason("Rate limit exceeded"));
            return;
        }

        try {
            ChessMessage parsed = PayloadRegistry.deserialize(message.getPayload());
            executor.submit(() -> sendMessage(session, router.route(parsed, user)));
        } catch (InvalidMessageException e) {
            log.error("[WS] Invalid message from {}: {}", user, e.getMessage());
            sendMessage(session, ChessMessage.error(e.getCode(), e.getMessage()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String user = user(session);
        if (user != null) {
            rateLimiter.removeSession(user);
            log.info("[WS] Connection closed: {}", user);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("[WS] Error: {}", exception.getMessage());
    }

    private static String user(WebSocketSession session) {
        return (String) session.getAttributes().get(USER_ATTR);
    }

    private void sendMessage(WebSocketSession session, ChessMessage message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(PayloadRegistry.serialize(message)));
            }
        } catch (IOException e) {
            log.error("[WS] Failed to send message: {}", e.getMessage(), e);
        }
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException e) {
            log.error("[WS] Failed to close session: {}", e.getMessage(), e);
        }
    }
}
