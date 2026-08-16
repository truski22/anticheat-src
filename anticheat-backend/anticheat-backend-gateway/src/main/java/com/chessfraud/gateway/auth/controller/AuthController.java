package com.chessfraud.gateway.auth.controller;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.chessfraud.gateway.auth.dto.AuthResponse;
import com.chessfraud.gateway.auth.dto.ChangePasswordRequestBody;
import com.chessfraud.gateway.auth.dto.ForgotPasswordRequestBody;
import com.chessfraud.gateway.auth.dto.LoginRequestBody;
import com.chessfraud.gateway.auth.dto.RegisterRequestBody;
import com.chessfraud.gateway.auth.dto.ResetPasswordRequestBody;
import com.chessfraud.gateway.auth.exception.AuthenticationException;
import com.chessfraud.gateway.auth.service.JwtService;
import com.chessfraud.gateway.auth.service.PasswordResetCodeStore;
import com.chessfraud.gateway.grpc.ServiceClients;
import com.chessfraud.grpc.user.ChangePasswordByEmailRequest;
import com.chessfraud.grpc.user.ChangePasswordByEmailResponse;
import com.chessfraud.grpc.user.ChangePasswordRequest;
import com.chessfraud.grpc.user.ChangePasswordResponse;
import com.chessfraud.grpc.user.LoginRequest;
import com.chessfraud.grpc.user.LoginResponse;
import com.chessfraud.grpc.user.RegisterRequest;
import com.chessfraud.grpc.user.RegisterResponse;
import com.chessfraud.grpc.user.SendPasswordResetEmailRequest;
import com.chessfraud.grpc.user.SendPasswordResetEmailResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final ServiceClients clients;
    private final JwtService jwtService;
    private final PasswordResetCodeStore resetCodes;

    public AuthController(ServiceClients clients, JwtService jwtService, PasswordResetCodeStore resetCodes) {
        this.clients = clients;
        this.jwtService = jwtService;
        this.resetCodes = resetCodes;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequestBody body) {
        String user = requireField(body.user(), "user");
        String password = requireField(body.password(), "password");

        try {
            LoginResponse response = clients.userService().login(
                LoginRequest.newBuilder().setUser(user).setPassword(password).build());

            String token = response.getSuccess() ? jwtService.generateToken(user) : null;
            return ResponseEntity.status(response.getSuccess() ? 200 : 401)
                .body(new AuthResponse(response.getSuccess(), response.getMessage(), token));
        } catch (StatusRuntimeException e) {
            return grpcErrorResponse(e, 401, "Authentication failed");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequestBody body) {
        String user = requireField(body.user(), "user");
        String email = requireField(body.email(), "email");
        String password = requireField(body.password(), "password");
        validateEmail(email);
        validatePassword(password);

        try {
            RegisterResponse response = clients.userService().register(
                RegisterRequest.newBuilder().setUser(user).setEmail(email).setPassword(password).build());

            String token = response.getSuccess() ? jwtService.generateToken(user) : null;
            return ResponseEntity.status(response.getSuccess() ? 201 : 409)
                .body(new AuthResponse(response.getSuccess(), response.getMessage(), token));
        } catch (StatusRuntimeException e) {
            return grpcErrorResponse(e, 409, "Registration failed");
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<AuthResponse> forgotPassword(@RequestBody ForgotPasswordRequestBody body) {
        String email = requireField(body.email(), "email");
        validateEmail(email);

        try {
            SendPasswordResetEmailResponse response = clients.userService().sendPasswordResetEmail(
                SendPasswordResetEmailRequest.newBuilder().setEmail(email).build());
            if (response.getSent()) {
                resetCodes.store(email, response.getCode());
            }

            return ResponseEntity.ok(
                new AuthResponse(true, "If the email exists, a reset code has been sent"));
        } catch (StatusRuntimeException e) {
            return grpcErrorResponse(e, 500, "Unable to process password reset request");
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@RequestBody ResetPasswordRequestBody body) {
        String email = requireField(body.email(), "email");
        String code = requireField(body.code(), "code");
        String password = requireField(body.password(), "password");
        validateEmail(email);
        validatePassword(password);

        if (!resetCodes.validateAndConsume(email, code)) {
            return ResponseEntity.status(403).body(new AuthResponse(false, "Invalid or expired reset code"));
        }

        try {
            ChangePasswordByEmailResponse response = clients.userService().changePasswordByEmail(
                ChangePasswordByEmailRequest.newBuilder().setEmail(email).setPassword(password).build());

            if (response.getSuccess()) {
                return ResponseEntity.ok(new AuthResponse(true, "Password updated"));
            }
            return ResponseEntity.status(500).body(new AuthResponse(false, "Password update failed"));
        } catch (StatusRuntimeException e) {
            return grpcErrorResponse(e, 500, "Password update failed");
        }
    }

    /**
     * Password change for an already-authenticated user. Unlike /auth/reset-password (which
     * proves email ownership with a code), here proof of ownership is the JWT: the username
     * that ChangePassword is called with ALWAYS comes from the validated token, never from a
     * "user" field in the body. Without this, anyone who knew another account's name could
     * change its password without proving they own it - the user-service .proto doesn't carry
     * caller identity, so this check has to live here.
     */
    @PostMapping("/change-password")
    public ResponseEntity<AuthResponse> changePassword(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                         @RequestBody ChangePasswordRequestBody body) {
        String authenticatedUser = requireAuthenticatedUser(authorization);
        String newPassword = requireField(body.password(), "password");
        validatePassword(newPassword);

        try {
            ChangePasswordResponse response = clients.userService().changePassword(
                ChangePasswordRequest.newBuilder().setUser(authenticatedUser).setPassword(newPassword).build());

            if (response.getSuccess()) {
                return ResponseEntity.ok(new AuthResponse(true, "Password updated"));
            }
            return ResponseEntity.status(409)
                .body(new AuthResponse(false, "New password must be different from the current one"));
        } catch (StatusRuntimeException e) {
            return grpcErrorResponse(e, 500, "Password update failed");
        }
    }

    /**
     * Extracts and validates the JWT from the {@code Authorization: Bearer <token>} header,
     * returning the username (subject) that signed the token. Trusts nothing from the request
     * body to identify the caller.
     */
    private String requireAuthenticatedUser(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AuthenticationException("Missing or malformed Authorization header");
        }
        String token = authorization.substring("Bearer ".length()).trim();
        if (token.isBlank()) {
            throw new AuthenticationException("Missing or malformed Authorization header");
        }
        try {
            return jwtService.validateToken(token);
        } catch (JWTVerificationException e) {
            throw new AuthenticationException("Invalid or expired token");
        }
    }

    private String requireField(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return value;
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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AuthResponse> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new AuthResponse(false, e.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<AuthResponse> handleUnauthenticated(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(false, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AuthResponse> handleUnexpected(Exception e) {
        log.error("[AUTH] Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity.status(500).body(new AuthResponse(false, "Internal server error"));
    }

    private ResponseEntity<AuthResponse> grpcErrorResponse(StatusRuntimeException e, int defaultStatus, String defaultMessage) {
        String message = grpcMessage(e, defaultMessage);
        int status = grpcHttpStatus(e.getStatus().getCode(), defaultStatus);
        log.error("[AUTH] gRPC error: {}", message, e);
        return ResponseEntity.status(status).body(new AuthResponse(false, message));
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
        return (description != null && !description.isBlank()) ? description : defaultMessage;
    }
}
