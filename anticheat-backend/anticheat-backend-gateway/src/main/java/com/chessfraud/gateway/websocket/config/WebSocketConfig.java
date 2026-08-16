package com.chessfraud.gateway.websocket.config;

import com.chessfraud.gateway.websocket.handler.ChessWebSocketHandler;
import com.chessfraud.gateway.websocket.interceptor.JwtHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final ChessWebSocketHandler handler;
    private final JwtHandshakeInterceptor interceptor;
    private final String[] allowedOrigins;

    public WebSocketConfig(ChessWebSocketHandler handler,
                            JwtHandshakeInterceptor interceptor,
                            @Value("${gateway.allowed-origins}") String[] allowedOrigins) {
        this.handler = handler;
        this.interceptor = interceptor;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws")
            .addInterceptors(interceptor)
            .setAllowedOrigins(allowedOrigins);
    }
}
