package auth;

import com.chessfraud.grpc.user.ChangePasswordByEmailRequest;
import com.chessfraud.grpc.user.ChangePasswordByEmailResponse;
import com.chessfraud.grpc.user.LoginRequest;
import com.chessfraud.grpc.user.LoginResponse;
import com.chessfraud.grpc.user.RegisterRequest;
import com.chessfraud.grpc.user.RegisterResponse;
import com.chessfraud.grpc.user.SendPasswordResetEmailRequest;
import com.chessfraud.grpc.user.SendPasswordResetEmailResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import grpc.ServiceClients;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class AuthController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthController.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_BODY_SIZE = 4096;
    private static final long RESET_CODE_TTL_SECONDS = 600;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final ServiceClients clients;
    private final JwtService jwtService;
    private final HttpServer httpServer;
    private final ScheduledExecutorService cleaner;
    private final Map<String, ResetCode> pendingResetCodes = new ConcurrentHashMap<>();

    private record ResetCode(String code, Instant expiresAt) {
        boolean isValid(String inputCode) {
            return code.equals(inputCode) && Instant.now().isBefore(expiresAt);
        }
    }

    public AuthController(ServiceClients clients, JwtService jwtService) throws IOException {
        this.clients = clients;
        this.jwtService = jwtService;

        int port = Integer.parseInt(System.getenv().getOrDefault("AUTH_PORT", "8081"));
        this.httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        this.httpServer.setExecutor(Executors.newFixedThreadPool(4));

        httpServer.createContext("/auth/login", new LoginHandler());
        httpServer.createContext("/auth/register", new RegisterHandler());
        httpServer.createContext("/auth/forgot-password", new ForgotPasswordHandler());
        httpServer.createContext("/auth/reset-password", new ResetPasswordHandler());
        httpServer.createContext("/health", exchange -> sendJson(exchange, 200, "{\"status\":\"ok\"}"));

        this.cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "reset-code-cleaner");
            thread.setDaemon(true);
            return thread;
        });
        this.cleaner.scheduleAtFixedRate(this::cleanExpiredCodes, 5, 5, TimeUnit.MINUTES);
    }

    public void start() {
        httpServer.start();
        log.info("[AUTH] REST server started on port {}", httpServer.getAddress().getPort());
    }

    public void stop() {
        cleaner.shutdownNow();
        httpServer.stop(1);
    }

    private void storeResetCode(String email, String code) {
        if (email == null || email.isBlank() || code == null || code.isBlank()) {
            return;
        }

        Instant expiry = Instant.now().plusSeconds(RESET_CODE_TTL_SECONDS);
        pendingResetCodes.put(email.toLowerCase(), new ResetCode(code, expiry));
        log.info("[AUTH] Reset code stored for email '{}', expires at {}", email, expiry);
    }

    private void cleanExpiredCodes() {
        pendingResetCodes.entrySet().removeIf(entry -> Instant.now().isAfter(entry.getValue().expiresAt()));
    }

    private class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange) || !requirePost(exchange)) {
                return;
            }

            try {
                JsonNode body = readBody(exchange);
                String user = requireField(body, "user");
                String password = requireField(body, "password");

                LoginResponse response = clients.userService().login(
                    LoginRequest.newBuilder().setUser(user).setPassword(password).build());

                ObjectNode json = MAPPER.createObjectNode();
                json.put("success", response.getSuccess());
                json.put("message", response.getMessage());
                if (response.getSuccess()) {
                    json.put("token", jwtService.generateToken(user));
                }
                sendJson(exchange, response.getSuccess() ? 200 : 401, MAPPER.writeValueAsString(json));
            } catch (IllegalArgumentException e) {
                sendJson(exchange, 400, errorJson(e.getMessage()));
            } catch (StatusRuntimeException e) {
                sendGrpcError(exchange, e, 401, "Authentication failed");
            } catch (Exception e) {
                log.error("[AUTH] Login failed: {}", e.getMessage(), e);
                sendJson(exchange, 500, errorJson("Internal server error"));
            }
        }
    }

    private class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange) || !requirePost(exchange)) {
                return;
            }

            try {
                JsonNode body = readBody(exchange);
                String user = requireField(body, "user");
                String email = requireField(body, "email");
                String password = requireField(body, "password");

                validateEmail(email);
                validatePassword(password);

                RegisterResponse response = clients.userService().register(
                    RegisterRequest.newBuilder()
                        .setUser(user)
                        .setEmail(email)
                        .setPassword(password)
                        .build());

                ObjectNode json = MAPPER.createObjectNode();
                json.put("success", response.getSuccess());
                json.put("message", response.getMessage());
                if (response.getSuccess()) {
                    json.put("token", jwtService.generateToken(user));
                }
                sendJson(exchange, response.getSuccess() ? 201 : 409, MAPPER.writeValueAsString(json));
            } catch (IllegalArgumentException e) {
                sendJson(exchange, 400, errorJson(e.getMessage()));
            } catch (StatusRuntimeException e) {
                sendGrpcError(exchange, e, 409, "Registration failed");
            } catch (Exception e) {
                log.error("[AUTH] Registration failed: {}", e.getMessage(), e);
                sendJson(exchange, 500, errorJson("Internal server error"));
            }
        }
    }

    private class ForgotPasswordHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange) || !requirePost(exchange)) {
                return;
            }

            try {
                JsonNode body = readBody(exchange);
                String email = requireField(body, "email");
                validateEmail(email);

                SendPasswordResetEmailResponse response = clients.userService().sendPasswordResetEmail(
                    SendPasswordResetEmailRequest.newBuilder().setEmail(email).build());
                if (response.getSent()) {
                    storeResetCode(email, response.getCode());
                }

                sendJson(exchange, 200,
                    "{\"success\":true,\"message\":\"If the email exists, a reset code has been sent\"}");
            } catch (IllegalArgumentException e) {
                sendJson(exchange, 400, errorJson(e.getMessage()));
            } catch (StatusRuntimeException e) {
                sendGrpcError(exchange, e, 500, "Unable to process password reset request");
            } catch (Exception e) {
                log.error("[AUTH] Forgot-password failed: {}", e.getMessage(), e);
                sendJson(exchange, 500, errorJson("Internal server error"));
            }
        }
    }

    private class ResetPasswordHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange) || !requirePost(exchange)) {
                return;
            }

            try {
                JsonNode body = readBody(exchange);
                String email = requireField(body, "email");
                String code = requireField(body, "code");
                String password = requireField(body, "password");

                validateEmail(email);
                validatePassword(password);

                ResetCode storedCode = pendingResetCodes.get(email.toLowerCase());
                if (storedCode == null || !storedCode.isValid(code)) {
                    pendingResetCodes.remove(email.toLowerCase());
                    sendJson(exchange, 403, errorJson("Invalid or expired reset code"));
                    return;
                }

                pendingResetCodes.remove(email.toLowerCase());
                ChangePasswordByEmailResponse response = clients.userService().changePasswordByEmail(
                    ChangePasswordByEmailRequest.newBuilder()
                        .setEmail(email)
                        .setPassword(password)
                        .build());

                if (response.getSuccess()) {
                    sendJson(exchange, 200, "{\"success\":true,\"message\":\"Password updated\"}");
                } else {
                    sendJson(exchange, 500, errorJson("Password update failed"));
                }
            } catch (IllegalArgumentException e) {
                sendJson(exchange, 400, errorJson(e.getMessage()));
            } catch (StatusRuntimeException e) {
                sendGrpcError(exchange, e, 500, "Password update failed");
            } catch (Exception e) {
                log.error("[AUTH] Reset-password failed: {}", e.getMessage(), e);
                sendJson(exchange, 500, errorJson("Internal server error"));
            }
        }
    }

    private JsonNode readBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            byte[] bytes = inputStream.readNBytes(MAX_BODY_SIZE);
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
            sendJson(exchange, 405, errorJson("Method not allowed"));
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

    private void sendGrpcError(HttpExchange exchange, StatusRuntimeException e, int defaultStatus, String defaultMessage)
        throws IOException {
        String message = grpcMessage(e, defaultMessage);
        int status = grpcHttpStatus(e.getStatus().getCode(), defaultStatus);
        log.error("[AUTH] gRPC error: {}", message, e);
        sendJson(exchange, status, errorJson(message));
    }

    private int grpcHttpStatus(Status.Code code, int defaultStatus) {
        return switch (code) {
            case INVALID_ARGUMENT -> 400;
            case UNAUTHENTICATED -> 401;
            case PERMISSION_DENIED -> 403;
            case NOT_FOUND -> 404;
            case ALREADY_EXISTS -> 409;
            case FAILED_PRECONDITION -> 412;
            case DEADLINE_EXCEEDED -> 504;
            case UNAVAILABLE -> 503;
            default -> defaultStatus;
        };
    }

    private String grpcMessage(StatusRuntimeException e, String defaultMessage) {
        String description = e.getStatus().getDescription();
        if (description != null && !description.isBlank()) {
            return description;
        }
        return defaultMessage;
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private static String errorJson(String message) {
        return "{\"success\":false,\"message\":\"" + escapeJson(message) + "\"}";
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
