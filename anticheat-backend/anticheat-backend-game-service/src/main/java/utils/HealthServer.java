package utils;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class HealthServer {
    private static final Logger log = LoggerFactory.getLogger(HealthServer.class);
    private static final byte[] BODY = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);

    private HealthServer() {
    }

    public static void start(int port) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            server.createContext("/health", exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, BODY.length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(BODY);
                }
            });
            Thread thread = new Thread(server::start, "health-server");
            thread.setDaemon(true);
            thread.start();
            log.info("[HEALTH] Health server started on port {}", port);
        } catch (IOException e) {
            log.error("[HEALTH] Failed to start health server on port {}: {}", port, e.getMessage(), e);
        }
    }
}
