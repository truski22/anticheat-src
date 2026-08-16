package com.chessfraud.userservice.user.grpc;

import com.chessfraud.grpc.user.LoginRequest;
import com.chessfraud.grpc.user.LoginResponse;
import com.chessfraud.grpc.user.RegisterRequest;
import com.chessfraud.grpc.user.RegisterResponse;
import com.chessfraud.grpc.user.UserInfoRequest;
import com.chessfraud.grpc.user.UserServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the actual contract consumed by the Gateway: same wire strings
 * ("OK"/"KO"/"UNV"/"ENV") and same gRPC Status for the error cases.
 * Runs against an in-process gRPC server (grpc.server.in-process-name in
 * src/test/resources/application.properties) and in-memory H2 — no network, no Postgres.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UserGrpcServiceTest {

    private ManagedChannel channel;
    private UserServiceGrpc.UserServiceBlockingStub stub;

    @BeforeEach
    void setUp() {
        channel = InProcessChannelBuilder.forName("test-user-service").directExecutor().build();
        stub = UserServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void registerThenLoginSucceeds() {
        RegisterResponse register = stub.register(RegisterRequest.newBuilder()
                .setUser("dana")
                .setEmail("dana@example.com")
                .setPassword("hunter2hunter2")
                .build());
        assertThat(register.getSuccess()).isTrue();
        assertThat(register.getMessage()).isEqualTo("OK");

        LoginResponse login = stub.login(LoginRequest.newBuilder()
                .setUser("dana")
                .setPassword("hunter2hunter2")
                .build());
        assertThat(login.getSuccess()).isTrue();
        assertThat(login.getMessage()).isEqualTo("OK");
    }

    @Test
    void loginWithWrongPasswordReturnsKo() {
        stub.register(RegisterRequest.newBuilder().setUser("erin").setEmail("erin@example.com").setPassword("hunter2hunter2").build());

        LoginResponse login = stub.login(LoginRequest.newBuilder().setUser("erin").setPassword("wrong-pass").build());

        assertThat(login.getSuccess()).isFalse();
        assertThat(login.getMessage()).isEqualTo("KO");
    }

    @Test
    void duplicateUsernameReturnsUnv() {
        stub.register(RegisterRequest.newBuilder().setUser("frank").setEmail("frank@example.com").setPassword("hunter2hunter2").build());

        RegisterResponse second = stub.register(RegisterRequest.newBuilder().setUser("frank").setEmail("other@example.com").setPassword("hunter2hunter2").build());

        assertThat(second.getSuccess()).isFalse();
        assertThat(second.getMessage()).isEqualTo("UNV");
    }

    @Test
    void duplicateEmailReturnsEnv() {
        stub.register(RegisterRequest.newBuilder().setUser("grace").setEmail("grace@example.com").setPassword("hunter2hunter2").build());

        RegisterResponse second = stub.register(RegisterRequest.newBuilder().setUser("other-grace").setEmail("grace@example.com").setPassword("hunter2hunter2").build());

        assertThat(second.getSuccess()).isFalse();
        assertThat(second.getMessage()).isEqualTo("ENV");
    }

    @Test
    void blankFieldsAreRejectedWithInvalidArgument() {
        assertThatThrownBy(() -> stub.register(RegisterRequest.newBuilder()
                .setUser("")
                .setEmail("x@example.com")
                .setPassword("hunter2hunter2")
                .build()))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(ex -> assertThat(((StatusRuntimeException) ex).getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT));
    }

    @Test
    void unknownUserInfoReturnsNotFound() {
        assertThatThrownBy(() -> stub.getUserInfo(UserInfoRequest.newBuilder().setUser("ghost").build()))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(ex -> assertThat(((StatusRuntimeException) ex).getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND));
    }
}
