package com.chessfraud.userservice.user.grpc;

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
import com.chessfraud.grpc.user.UserInfoRequest;
import com.chessfraud.grpc.user.UserInfoResponse;
import com.chessfraud.grpc.user.UserServiceGrpc;
import com.chessfraud.userservice.email.service.EmailNotificationService;
import com.chessfraud.userservice.user.dto.LoginOutcome;
import com.chessfraud.userservice.user.dto.LoginResult;
import com.chessfraud.userservice.user.dto.RegisterOutcome;
import com.chessfraud.userservice.user.dto.RegisterResult;
import com.chessfraud.userservice.user.dto.UserInfoResult;
import com.chessfraud.userservice.user.service.UserAccountService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.security.SecureRandom;

/**
 * Implements the contract from anticheat-backend-libs/grpc-api/user_service.proto
 * (same default host/port "user-service:9090" expected by
 * anticheat-backend-gateway's ServiceClients). Replaces the old
 * {@code web/UserController.java} REST controller: same business routes,
 * gRPC transport instead of JSON over HTTP.
 */
@GrpcService
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserAccountService accountService;
    private final EmailNotificationService emailService;

    public UserGrpcService(UserAccountService accountService, EmailNotificationService emailService) {
        this.accountService = accountService;
        this.emailService = emailService;
    }

    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        if (isBlank(request.getUser()) || isBlank(request.getPassword())) {
            responseObserver.onError(invalidArgument("user and password are required"));
            return;
        }
        LoginResult result = accountService.login(request.getUser(), request.getPassword());
        responseObserver.onNext(LoginResponse.newBuilder()
                .setSuccess(result.isSuccess())
                .setMessage(toWireMessage(result.getOutcome()))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        if (isBlank(request.getUser()) || isBlank(request.getEmail()) || isBlank(request.getPassword())) {
            responseObserver.onError(invalidArgument("user, email and password are required"));
            return;
        }
        RegisterResult result = accountService.register(request.getUser(), request.getEmail(), request.getPassword());
        responseObserver.onNext(RegisterResponse.newBuilder()
                .setSuccess(result.isSuccess())
                .setMessage(toWireMessage(result.getOutcome()))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getUserInfo(UserInfoRequest request, StreamObserver<UserInfoResponse> responseObserver) {
        if (isBlank(request.getUser())) {
            responseObserver.onError(invalidArgument("user is required"));
            return;
        }
        UserInfoResult result = accountService.getUserInfo(request.getUser());
        if (result == null) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("User not found").asRuntimeException());
            return;
        }
        responseObserver.onNext(UserInfoResponse.newBuilder()
                .setUser(request.getUser())
                .setEmail(result.getEmail())
                .setTotalGames(result.getTotalGames())
                .setCheatGames(result.getCheatedGames())
                .setLegalGames(result.getFairGames())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void changePassword(ChangePasswordRequest request, StreamObserver<ChangePasswordResponse> responseObserver) {
        if (isBlank(request.getUser()) || isBlank(request.getPassword())) {
            responseObserver.onError(invalidArgument("user and password are required"));
            return;
        }
        boolean changed = accountService.changePassword(request.getUser(), request.getPassword());
        responseObserver.onNext(ChangePasswordResponse.newBuilder().setSuccess(changed).build());
        responseObserver.onCompleted();
    }

    @Override
    public void changePasswordByEmail(ChangePasswordByEmailRequest request, StreamObserver<ChangePasswordByEmailResponse> responseObserver) {
        if (isBlank(request.getEmail()) || isBlank(request.getPassword())) {
            responseObserver.onError(invalidArgument("email and password are required"));
            return;
        }
        boolean changed = accountService.changePasswordByEmail(request.getEmail(), request.getPassword());
        responseObserver.onNext(ChangePasswordByEmailResponse.newBuilder().setSuccess(changed).build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendPasswordResetEmail(SendPasswordResetEmailRequest request, StreamObserver<SendPasswordResetEmailResponse> responseObserver) {
        if (isBlank(request.getEmail())) {
            responseObserver.onError(invalidArgument("email is required"));
            return;
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
        responseObserver.onNext(SendPasswordResetEmailResponse.newBuilder()
                .setSent(sent)
                .setCode(code)
                .build());
        responseObserver.onCompleted();
    }

    /**
     * The only place in the code that knows about the "OK"/"UNV"/"ENV"/"KO"
     * literals that fix the contract with the Gateway. The rest of the app
     * (UserAccountService, RegisterResult/LoginResult) works with the
     * RegisterOutcome/LoginOutcome enums — no comparing against these
     * strings outside of here.
     */
    private String toWireMessage(RegisterOutcome outcome) {
        return switch (outcome) {
            case OK -> "OK";
            case USERNAME_TAKEN -> "UNV";
            case EMAIL_TAKEN -> "ENV";
        };
    }

    private String toWireMessage(LoginOutcome outcome) {
        return switch (outcome) {
            case OK -> "OK";
            case INVALID_CREDENTIALS -> "KO";
        };
    }

    private String generateRandomCode() {
        int code = 100_000 + SECURE_RANDOM.nextInt(900_000);
        return String.valueOf(code);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private RuntimeException invalidArgument(String message) {
        return Status.INVALID_ARGUMENT.withDescription(message).asRuntimeException();
    }
}
