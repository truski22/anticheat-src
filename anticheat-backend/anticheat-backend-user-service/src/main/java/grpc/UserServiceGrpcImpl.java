package grpc;

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
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.EmailNotificationService;
import service.LoginResult;
import service.RegisterResult;
import service.UserAccountService;
import service.UserInfoResult;

import java.security.SecureRandom;

public class UserServiceGrpcImpl extends UserServiceGrpc.UserServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(UserServiceGrpcImpl.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final UserAccountService accountService;
    private final EmailNotificationService emailService;

    public UserServiceGrpcImpl(UserAccountService accountService, EmailNotificationService emailService) {
        this.accountService = accountService;
        this.emailService = emailService;
    }

    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        if (isBlank(request.getUser()) || isBlank(request.getPassword())) {
            invalidArgument(responseObserver, "user and password are required");
            return;
        }

        try {
            LoginResult result = accountService.login(request.getUser(), request.getPassword());
            complete(responseObserver, LoginResponse.newBuilder()
                    .setSuccess(result.isSuccess())
                    .setMessage(result.getResponse())
                    .build());
        } catch (RuntimeException e) {
            internalError("login", responseObserver, e);
        }
    }

    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        if (isBlank(request.getUser()) || isBlank(request.getEmail()) || isBlank(request.getPassword())) {
            invalidArgument(responseObserver, "user, email and password are required");
            return;
        }

        try {
            RegisterResult result = accountService.register(request.getUser(), request.getEmail(), request.getPassword());
            complete(responseObserver, RegisterResponse.newBuilder()
                    .setSuccess(result.isSuccess())
                    .setMessage(result.getResponse())
                    .build());
        } catch (RuntimeException e) {
            internalError("register", responseObserver, e);
        }
    }

    @Override
    public void getUserInfo(UserInfoRequest request, StreamObserver<UserInfoResponse> responseObserver) {
        if (isBlank(request.getUser())) {
            invalidArgument(responseObserver, "user is required");
            return;
        }

        try {
            UserInfoResult result = accountService.getUserInfo(request.getUser());
            if (result == null) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("User not found").asRuntimeException());
                return;
            }

            complete(responseObserver, UserInfoResponse.newBuilder()
                    .setUser(request.getUser())
                    .setEmail(result.getEmail())
                    .setTotalGames(result.getTotalGames())
                    .setCheatGames(result.getCheatedGames())
                    .setLegalGames(result.getFairGames())
                    .build());
        } catch (RuntimeException e) {
            internalError("getUserInfo", responseObserver, e);
        }
    }

    @Override
    public void changePassword(ChangePasswordRequest request,
                               StreamObserver<ChangePasswordResponse> responseObserver) {
        if (isBlank(request.getUser()) || isBlank(request.getPassword())) {
            invalidArgument(responseObserver, "user and password are required");
            return;
        }

        try {
            boolean changed = accountService.changePassword(request.getUser(), request.getPassword());
            complete(responseObserver, ChangePasswordResponse.newBuilder()
                    .setSuccess(changed)
                    .build());
        } catch (RuntimeException e) {
            internalError("changePassword", responseObserver, e);
        }
    }

    @Override
    public void changePasswordByEmail(ChangePasswordByEmailRequest request,
                                      StreamObserver<ChangePasswordByEmailResponse> responseObserver) {
        if (isBlank(request.getEmail()) || isBlank(request.getPassword())) {
            invalidArgument(responseObserver, "email and password are required");
            return;
        }

        try {
            boolean changed = accountService.changePasswordByEmail(request.getEmail(), request.getPassword());
            complete(responseObserver, ChangePasswordByEmailResponse.newBuilder()
                    .setSuccess(changed)
                    .build());
        } catch (RuntimeException e) {
            internalError("changePasswordByEmail", responseObserver, e);
        }
    }

    @Override
    public void sendPasswordResetEmail(SendPasswordResetEmailRequest request,
                                       StreamObserver<SendPasswordResetEmailResponse> responseObserver) {
        if (isBlank(request.getEmail())) {
            invalidArgument(responseObserver, "email is required");
            return;
        }

        try {
            String code = "";
            boolean sent = false;
            if (accountService.emailExists(request.getEmail())) {
                code = generateRandomCode();
                sent = emailService.sendVerificationCode(request.getEmail(), code);
                if (!sent) {
                    code = "";
                }
            }

            complete(responseObserver, SendPasswordResetEmailResponse.newBuilder()
                    .setSent(sent)
                    .setCode(code)
                    .build());
        } catch (RuntimeException e) {
            internalError("sendPasswordResetEmail", responseObserver, e);
        }
    }

    private String generateRandomCode() {
        int code = 100_000 + SECURE_RANDOM.nextInt(900_000);
        return String.valueOf(code);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private <T> void complete(StreamObserver<T> responseObserver, T response) {
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void invalidArgument(StreamObserver<?> responseObserver, String description) {
        responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(description).asRuntimeException());
    }

    private void internalError(String operation, StreamObserver<?> responseObserver, RuntimeException exception) {
        log.error("[GRPC] {} failed: {}", operation, exception.getMessage(), exception);
        responseObserver.onError(Status.INTERNAL.withDescription(operation + " failed").asRuntimeException());
    }
}
