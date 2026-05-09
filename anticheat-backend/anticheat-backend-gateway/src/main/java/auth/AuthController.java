package auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import mqtt.*;
import protocol.payload.ResponsePayload;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class AuthController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthController.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int AUTH_TIMEOUT_SECONDS = 10;
    private static final int MAX_BODY_SIZE = 4096;
    private static final long RESET_CODE_TTL_SECONDS = 600; // 10 minutes
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final MqttInterface mqttInterface;
    private final JwtService jwtService;
    private final HttpServer httpServer;

    // Pending auth requests: correlationKey -> future
    private final Map<String, CompletableFuture<ResponsePayload>> pendingRequests = new ConcurrentHashMap<>();

    // Password reset codes: email -> ResetCode (code + expiry)
    private final Map<String, ResetCode> pendingResetCodes = new ConcurrentHashMap<>();

    private record ResetCode(String code, Instant expiresAt) {
        boolean isValid(String inputCode) {
            return code.equals(inputCode) && Instant.now().isBefore(expiresAt);
        }
    }

    public AuthController(MqttInterface mqttInterface, JwtService jwtService) throws IOException {
        this.mqttInterface = mqttInterface;
        this.jwtService = jwtService;

        int port = Integer.parseInt(System.getenv().getOrDefault("AUTH_PORT", "8081"));
        this.httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        httpServer.setExecutor(Executors.newFixedThreadPool(4));

        httpServer.createContext("/auth/login", new LoginHandler());
        httpServer.createContext("/auth/register", new RegisterHandler());
        httpServer.createContext("/auth/forgot-password", new ForgotPasswordHandler());
        httpServer.createContext("/auth/reset-password", new ResetPasswordHandler());
        httpServer.createContext("/health", exchange -> sendJson(exchange, 200, "{\"status\":\"ok\"}"));

        // Periodically clean expired reset codes
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "reset-code-cleaner");
            t.setDaemon(true);
            return t;
        });
        cleaner.scheduleAtFixedRate(this::cleanExpiredCodes, 5, 5, TimeUnit.MINUTES);
    }

    public void start() {
        httpServer.start();
        log.info("[AUTH] REST server started on port {}", httpServer.getAddress().getPort());
    }

    public void stop() {
        httpServer.stop(1);
    }

    /**
     * Called by Logic when an MQTT auth response arrives. Completes the pending future.
     */
    public void completeAuthRequest(String correlationKey, ResponsePayload response) {
        CompletableFuture<ResponsePayload> future = pendingRequests.remove(correlationKey);
        if (future != null) {
            future.complete(response);
        }
    }

    /**
     * Called by ChangePasswordEmailResponseHandler when the user-service sends back the reset code.
     * Stores the code with TTL for later verification.
     */
    public void storeResetCode(String email, String code) {
        if (email != null && code != null) {
            Instant expiry = Instant.now().plusSeconds(RESET_CODE_TTL_SECONDS);
            pendingResetCodes.put(email.toLowerCase(), new ResetCode(code, expiry));
            log.info("[AUTH] Reset code stored for email '{}', expires at {}", email, expiry);
        }
    }

    private void cleanExpiredCodes() {
        pendingResetCodes.entrySet().removeIf(e -> Instant.now().isAfter(e.getValue().expiresAt()));
    }

    private CompletableFuture<ResponsePayload> submitRequest(String correlationKey, Common mqttMessage) {
        CompletableFuture<ResponsePayload> future = new CompletableFuture<>();
        pendingRequests.put(correlationKey, future);
        mqttInterface.publishMessage(mqttMessage);
        // Auto-cleanup on timeout
        future.orTimeout(AUTH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
              .whenComplete((r, ex) -> pendingRequests.remove(correlationKey));
        return future;
    }

    // ── Handlers ──────────────────────────────────────────────────────────

    private class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) return;
            if (!requirePost(exchange)) return;

            try {
                JsonNode body = readBody(exchange);
                String user = requireField(body, "user");
                String password = requireField(body, "password");

                String correlationKey = "login:" + UUID.randomUUID();
                LoginRequest req = new LoginRequest();
                req.setUser(user);
                req.setPassword(password);
                req.setMessageType("LoginRequest");
                req.setIdMessage(correlationKey);

                CompletableFuture<ResponsePayload> future = submitRequest(correlationKey, req);
                ResponsePayload response = future.get(AUTH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                ObjectNode json = MAPPER.createObjectNode();
                json.put("success", response.success());
                json.put("message", response.message());
                if (response.success()) {
                    json.put("token", jwtService.generateToken(user));
                }
                sendJson(exchange, response.success() ? 200 : 401, MAPPER.writeValueAsString(json));
            } catch (TimeoutException e) {
                sendJson(exchange, 504, "{\"success\":false,\"message\":\"Authentication timeout\"}");
            } catch (IllegalArgumentException e) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            } catch (Exception e) {
                sendJson(exchange, 500, "{\"success\":false,\"message\":\"Internal server error\"}");
            }
        }
    }

    private class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) return;
            if (!requirePost(exchange)) return;

            try {
                JsonNode body = readBody(exchange);
                String user = requireField(body, "user");
                String email = requireField(body, "email");
                String password = requireField(body, "password");

                validateEmail(email);
                validatePassword(password);

                String correlationKey = "register:" + UUID.randomUUID();
                RegisterRequest req = new RegisterRequest();
                req.setUser(user);
                req.setEmail(email);
                req.setPassword(password);
                req.setMessageType("RegisterRequest");
                req.setIdMessage(correlationKey);

                CompletableFuture<ResponsePayload> future = submitRequest(correlationKey, req);
                ResponsePayload response = future.get(AUTH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                ObjectNode json = MAPPER.createObjectNode();
                json.put("success", response.success());
                json.put("message", response.message());
                if (response.success()) {
                    json.put("token", jwtService.generateToken(user));
                }
                sendJson(exchange, response.success() ? 201 : 409, MAPPER.writeValueAsString(json));
            } catch (TimeoutException e) {
                sendJson(exchange, 504, "{\"success\":false,\"message\":\"Registration timeout\"}");
            } catch (IllegalArgumentException e) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            } catch (Exception e) {
                sendJson(exchange, 500, "{\"success\":false,\"message\":\"Internal server error\"}");
            }
        }
    }

    private class ForgotPasswordHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) return;
            if (!requirePost(exchange)) return;

            try {
                JsonNode body = readBody(exchange);
                String email = requireField(body, "email");
                validateEmail(email);

                ChangePasswordSendEmail req = new ChangePasswordSendEmail();
                req.setEmail(email);
                req.setMessageType("ChangePasswordSendEmail");

                mqttInterface.publishMessage(req);
                // Always return success to prevent user enumeration
                sendJson(exchange, 200, "{\"success\":true,\"message\":\"If the email exists, a reset code has been sent\"}");
            } catch (IllegalArgumentException e) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            } catch (Exception e) {
                sendJson(exchange, 500, "{\"success\":false,\"message\":\"Internal server error\"}");
            }
        }
    }

    private class ResetPasswordHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) return;
            if (!requirePost(exchange)) return;

            try {
                JsonNode body = readBody(exchange);
                String email = requireField(body, "email");
                String code = requireField(body, "code");
                String password = requireField(body, "password");

                validatePassword(password);

                // Verify the reset code
                ResetCode stored = pendingResetCodes.get(email.toLowerCase());
                if (stored == null || !stored.isValid(code)) {
                    pendingResetCodes.remove(email.toLowerCase());
                    sendJson(exchange, 403, "{\"success\":false,\"message\":\"Invalid or expired reset code\"}");
                    return;
                }

                // Code is valid — consume it (single use)
                pendingResetCodes.remove(email.toLowerCase());

                ChangePasswordEmail req = new ChangePasswordEmail();
                req.setEmail(email);
                req.setPassword(password);
                req.setMessageType("ChangePasswordEmail");

                mqttInterface.publishMessage(req);
                sendJson(exchange, 200, "{\"success\":true,\"message\":\"Password updated\"}");
            } catch (IllegalArgumentException e) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            } catch (Exception e) {
                sendJson(exchange, 500, "{\"success\":false,\"message\":\"Internal server error\"}");
            }
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────

    private JsonNode readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            byte[] bytes = is.readNBytes(MAX_BODY_SIZE);
            return MAPPER.readTree(bytes);
        }
    }

    private String requireField(JsonNode body, String field) {
        if (body == null || !body.has(field) || body.get(field).asText().isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return body.get(field).asText();
    }

    private void validateEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    private void validatePassword(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
    }

    private boolean requirePost(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
            return false;
        }
        return true;
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private boolean handlePreflight(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
