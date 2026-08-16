package com.chessfraud.gateway.grpc;

import com.chessfraud.grpc.analysis.AnalysisServiceGrpc;
import com.chessfraud.grpc.game.GameServiceGrpc;
import com.chessfraud.grpc.user.UserServiceGrpc;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * gRPC clients for user-service/game-service/analysis-service. Every stub returned here already
 * carries a deadline, and every {@code callX} helper wraps the call in a per-service circuit
 * breaker - without either, a stuck or repeatedly-failing downstream service can block gateway
 * threads indefinitely or keep taking traffic it has no chance of serving.
 */
@Component
public class ServiceClients {
    private static final Logger log = LoggerFactory.getLogger(ServiceClients.class);

    private static final long DEFAULT_DEADLINE_SECONDS = 3;
    // analysis-service can fall through to ml-service on a cache miss, which is slower than a
    // plain DB-backed call - it gets a longer deadline than user-service/game-service.
    private static final long ANALYSIS_DEADLINE_SECONDS = 15;

    private final ManagedChannel userChannel;
    private final ManagedChannel gameChannel;
    private final ManagedChannel analysisChannel;

    private final UserServiceGrpc.UserServiceBlockingStub userService;
    private final GameServiceGrpc.GameServiceBlockingStub gameService;
    private final AnalysisServiceGrpc.AnalysisServiceBlockingStub analysisService;

    private final CircuitBreaker userCircuitBreaker;
    private final CircuitBreaker gameCircuitBreaker;
    private final CircuitBreaker analysisCircuitBreaker;

    public ServiceClients(
            @Value("${gateway.services.user.host}") String userHost,
            @Value("${gateway.services.user.grpc-port}") int userPort,
            @Value("${gateway.services.game.host}") String gameHost,
            @Value("${gateway.services.game.grpc-port}") int gamePort,
            @Value("${gateway.services.analysis.host}") String analysisHost,
            @Value("${gateway.services.analysis.grpc-port}") int analysisPort,
            CircuitBreakerRegistry circuitBreakerRegistry) {

        this.userChannel = ManagedChannelBuilder.forAddress(userHost, userPort).usePlaintext().build();
        this.gameChannel = ManagedChannelBuilder.forAddress(gameHost, gamePort).usePlaintext().build();
        this.analysisChannel = ManagedChannelBuilder.forAddress(analysisHost, analysisPort).usePlaintext().build();

        this.userService = UserServiceGrpc.newBlockingStub(userChannel);
        this.gameService = GameServiceGrpc.newBlockingStub(gameChannel);
        this.analysisService = AnalysisServiceGrpc.newBlockingStub(analysisChannel);

        this.userCircuitBreaker = circuitBreakerRegistry.circuitBreaker("user-service");
        this.gameCircuitBreaker = circuitBreakerRegistry.circuitBreaker("game-service");
        this.analysisCircuitBreaker = circuitBreakerRegistry.circuitBreaker("analysis-service");

        log.info("[GRPC] Clients initialized: user={}:{}, game={}:{}, analysis={}:{}",
            userHost, userPort, gameHost, gamePort, analysisHost, analysisPort);
    }

    /** Blocking stub with a per-call deadline attached - never call the raw stub fields directly. */
    public UserServiceGrpc.UserServiceBlockingStub userService() {
        return userService.withDeadlineAfter(DEFAULT_DEADLINE_SECONDS, TimeUnit.SECONDS);
    }

    public GameServiceGrpc.GameServiceBlockingStub gameService() {
        return gameService.withDeadlineAfter(DEFAULT_DEADLINE_SECONDS, TimeUnit.SECONDS);
    }

    public AnalysisServiceGrpc.AnalysisServiceBlockingStub analysisService() {
        return analysisService.withDeadlineAfter(ANALYSIS_DEADLINE_SECONDS, TimeUnit.SECONDS);
    }

    /** Runs {@code call} through the user-service circuit breaker. */
    public <T> T callUser(Supplier<T> call) {
        return callThrough(userCircuitBreaker, call);
    }

    /** Runs {@code call} through the game-service circuit breaker. */
    public <T> T callGame(Supplier<T> call) {
        return callThrough(gameCircuitBreaker, call);
    }

    /** Runs {@code call} through the analysis-service circuit breaker. */
    public <T> T callAnalysis(Supplier<T> call) {
        return callThrough(analysisCircuitBreaker, call);
    }

    private <T> T callThrough(CircuitBreaker circuitBreaker, Supplier<T> call) {
        try {
            return circuitBreaker.executeSupplier(call);
        } catch (CallNotPermittedException e) {
            // Same error shape callers already handle for a downstream gRPC UNAVAILABLE -
            // no separate error path needed for "circuit open" vs. "service actually down".
            throw Status.UNAVAILABLE
                .withDescription("Service temporarily unavailable (circuit breaker open)")
                .withCause(e)
                .asRuntimeException();
        }
    }

    @PreDestroy
    void shutdown() {
        userChannel.shutdown();
        gameChannel.shutdown();
        analysisChannel.shutdown();
        try {
            userChannel.awaitTermination(5, TimeUnit.SECONDS);
            gameChannel.awaitTermination(5, TimeUnit.SECONDS);
            analysisChannel.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
