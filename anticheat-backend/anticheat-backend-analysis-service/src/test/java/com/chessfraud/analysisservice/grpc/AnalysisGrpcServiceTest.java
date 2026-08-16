package com.chessfraud.analysisservice.grpc;

import com.chessfraud.grpc.analysis.AnalysisServiceGrpc;
import com.chessfraud.grpc.analysis.AnalyzeGameRequest;
import com.chessfraud.grpc.analysis.AnalyzeGameResponse;
import com.sun.net.httpserver.HttpServer;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the actual contract consumed by the Gateway (AnalysisService.AnalyzeGame),
 * including the SHA-256 result cache. Runs against an in-process gRPC server
 * (grpc.server.in-process-name in src/test/resources/application.properties) and a
 * lightweight embedded HTTP stub standing in for ml-service — no real network call, no
 * dependency on Stockfish/ml-service being up.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AnalysisGrpcServiceTest {

    private static HttpServer stubServer;
    private static final AtomicInteger requestCount = new AtomicInteger();
    private static volatile String lastAuthHeader;

    @DynamicPropertySource
    static void mlServiceStub(DynamicPropertyRegistry registry) throws IOException {
        stubServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        stubServer.createContext("/predict", exchange -> {
            requestCount.incrementAndGet();
            lastAuthHeader = exchange.getRequestHeaders().getFirst("Authorization");
            exchange.getRequestBody().readAllBytes();

            String body = "{\"white\":{\"prediction\":\"0\"},\"black\":{\"prediction\":\"1\"},"
                    + "\"data_white\":[10,20],\"data_black\":[5,6]}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        stubServer.start();
        int port = stubServer.getAddress().getPort();
        registry.add("analysis-service.chess-insights-url", () -> "http://localhost:" + port + "/predict");
    }

    @AfterAll
    static void stopStub() {
        stubServer.stop(0);
    }

    private ManagedChannel channel;
    private AnalysisServiceGrpc.AnalysisServiceBlockingStub stub;

    @BeforeEach
    void setUp() {
        channel = InProcessChannelBuilder.forName("test-analysis-service").directExecutor().build();
        stub = AnalysisServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void analyzeGameReturnsMlPrediction() {
        AnalyzeGameResponse response = stub.analyzeGame(AnalyzeGameRequest.newBuilder()
                .setUser("carol")
                .setMoves("e2e4 e7e5")
                .build());

        assertThat(response.getWhiteLegal()).isTrue();
        assertThat(response.getBlackLegal()).isFalse();
        assertThat(response.getWhiteList()).containsExactly(10, 20);
        assertThat(response.getBlackList()).containsExactly(5, 6);
        assertThat(lastAuthHeader).isEqualTo("Bearer test-token");
    }

    @Test
    void repeatedMovesAreCachedAndOnlyHitMlServiceOnce() {
        AnalyzeGameRequest request = AnalyzeGameRequest.newBuilder()
                .setUser("dave")
                .setMoves("d2d4 d7d5 " + System.nanoTime())
                .build();

        int before = requestCount.get();
        stub.analyzeGame(request);
        stub.analyzeGame(request);
        assertThat(requestCount.get() - before).isEqualTo(1);
    }

    @Test
    void blankFieldsAreRejectedWithInvalidArgument() {
        assertThatThrownBy(() -> stub.analyzeGame(AnalyzeGameRequest.newBuilder()
                .setUser("")
                .setMoves("e2e4")
                .build()))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(ex -> assertThat(((StatusRuntimeException) ex).getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT));
    }
}
