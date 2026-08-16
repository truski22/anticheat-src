package com.chessfraud.userservice.web;

import com.chessfraud.userservice.dto.ChangePasswordByEmailRequest;
import com.chessfraud.userservice.dto.ChangePasswordRequest;
import com.chessfraud.userservice.dto.ChangePasswordResponse;
import com.chessfraud.userservice.dto.ErrorResponse;
import com.chessfraud.userservice.dto.LoginRequest;
import com.chessfraud.userservice.dto.LoginResponse;
import com.chessfraud.userservice.dto.PasswordResetRequest;
import com.chessfraud.userservice.dto.PasswordResetResponse;
import com.chessfraud.userservice.dto.RegisterRequest;
import com.chessfraud.userservice.dto.RegisterResponse;
import com.chessfraud.userservice.dto.UserInfoResponse;
import com.chessfraud.userservice.dto.user.LoginResult;
import com.chessfraud.userservice.dto.user.RegisterResult;
import com.chessfraud.userservice.dto.user.UserInfoResult;
import main.java.com.chessfraud.userservice.service.email.EmailNotificationService;
import com.chessfraud.userservice.service.user.UserAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;

/**
 * Replaces the hand-rolled {@code http/UserHttpServer} + {@code http/UserHttpHandler}:
 * same routes, same behaviour, JSON bodies via Jackson instead of manual form parsing.
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserAccountService accountService;
    private final EmailNotificationService emailService;

    public UserController(UserAccountService accountService, EmailNotificationService emailService) {
        this.accountService = accountService;
        this.emailService = emailService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (isBlank(request.getName()) || isBlank(request.getEmail()) || isBlank(request.getPassword())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("user, email and password are required"));
        }
        RegisterResult result = accountService.register(request.getName(), request.getEmail(), request.getPassword());
        return ResponseEntity.ok(new RegisterResponse(result.isSuccess(), result.getResponse()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (isBlank(request.getName()) || isBlank(request.getPassword())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("user and password are required"));
        }
        LoginResult result = accountService.login(request.getName(), request.getPassword());
        return ResponseEntity.ok(new LoginResponse(result.isSuccess(), result.getResponse()));
    }

    @GetMapping("/info")
    public ResponseEntity<?> getUserInfo(@RequestParam("user") String user) {
        if (isBlank(user)) {
            return ResponseEntity.badRequest().body(new ErrorResponse("user is required"));
        }
        UserInfoResult result = accountService.getUserInfo(user);
        if (result == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("User not found"));
        }
        return ResponseEntity.ok(new UserInfoResponse(
                user, result.getEmail(), result.getTotalGames(), result.getCheatedGames(), result.getFairGames()));
    }

    @PostMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        if (isBlank(request.getName()) || isBlank(request.getPassword())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("user and password are required"));
        }
        boolean changed = accountService.changePassword(request.getName(), request.getPassword());
        return ResponseEntity.ok(new ChangePasswordResponse(changed));
    }

    @PostMapping("/password/by-email")
    public ResponseEntity<?> changePasswordByEmail(@RequestBody ChangePasswordByEmailRequest request) {
        if (isBlank(request.getEmail()) || isBlank(request.getPassword())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("email and password are required"));
        }
        boolean changed = accountService.changePasswordByEmail(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(new ChangePasswordResponse(changed));
    }

    @PostMapping("/password/reset-email")
    public ResponseEntity<?> sendPasswordResetEmail(@RequestBody PasswordResetRequest request) {
        if (isBlank(request.getEmail())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("email is required"));
        }

        String code = "";
        boolean sent = false;
        if (accountService.emailExists(request.getEmail())) {
            code = generateRandomCode();
            sent = emailService.sendVerificationCode(request.getEmail(), code);
            if (!sent) {
                code = "";
            }
        }
        return ResponseEntity.ok(new PasswordResetResponse(sent, code));
    }

    private String generateRandomCode() {
        int code = 100_000 + SECURE_RANDOM.nextInt(900_000);
        return String.valueOf(code);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
