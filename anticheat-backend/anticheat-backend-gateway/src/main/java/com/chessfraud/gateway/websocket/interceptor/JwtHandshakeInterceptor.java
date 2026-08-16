package com.chessfraud.gateway.websocket.interceptor;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.chessfraud.gateway.auth.service.JwtService;
import com.chessfraud.gateway.websocket.handler.ChessWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * Authenticates the WebSocket handshake via a {@code ?token=JWT} query parameter, since
 * browsers can't set custom headers on a WebSocket upgrade request. Rejects the handshake
 * (HTTP 401, connection never upgrades) if the token is missing or invalid.
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {
    private static final Logger log = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);

    private final JwtService jwtService;

    public JwtHandshakeInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        List<String> tokens = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().get("token");
        if (tokens == null || tokens.isEmpty()) {
            log.error("[WS] Handshake rejected: missing token query parameter");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            String user = jwtService.validateToken(tokens.get(0));
            attributes.put(ChessWebSocketHandler.USER_ATTR, user);
            return true;
        } catch (JWTVerificationException e) {
            log.error("[WS] JWT validation failed: {}", e.getMessage());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
