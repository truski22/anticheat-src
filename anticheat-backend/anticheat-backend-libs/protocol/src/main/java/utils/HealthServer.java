package utils;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/**
 * Lightweight HTTP health server for standalone Java services.
 * Exposes GET /health → 200 OK {"status":"ok"} on a configurable port.
 * Starts in a daemon thread so it does not block process shutdown.
 *
 * Usage: HealthServer.start(9090);
 */
public final class HealthServer {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HealthServer.class);
    private static final byte[] BODY = "{\"status\":\"ok\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    private HealthServer() {}

    public static void start(int port) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            server.createContext("/health", exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, BODY.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(BODY);
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
