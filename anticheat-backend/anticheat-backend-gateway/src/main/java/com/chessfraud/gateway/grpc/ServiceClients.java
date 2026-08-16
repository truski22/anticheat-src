package com.chessfraud.gateway.grpc;

import com.chessfraud.grpc.analysis.AnalysisServiceGrpc;
import com.chessfraud.grpc.game.GameServiceGrpc;
import com.chessfraud.grpc.user.UserServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class ServiceClients {
    private static final Logger log = LoggerFactory.getLogger(ServiceClients.class);

    private final ManagedChannel userChannel;
    private final ManagedChannel gameChannel;
    private final ManagedChannel analysisChannel;

    private final UserServiceGrpc.UserServiceBlockingStub userService;
    private final GameServiceGrpc.GameServiceBlockingStub gameService;
    private final AnalysisServiceGrpc.AnalysisServiceBlockingStub analysisService;

    public ServiceClients(
            @Value("${gateway.services.user.host}") String userHost,
            @Value("${gateway.services.user.grpc-port}") int userPort,
            @Value("${gateway.services.game.host}") String gameHost,
            @Value("${gateway.services.game.grpc-port}") int gamePort,
            @Value("${gateway.services.analysis.host}") String analysisHost,
            @Value("${gateway.services.analysis.grpc-port}") int analysisPort) {

        this.userChannel = ManagedChannelBuilder.forAddress(userHost, userPort).usePlaintext().build();
        this.gameChannel = ManagedChannelBuilder.forAddress(gameHost, gamePort).usePlaintext().build();
        this.analysisChannel = ManagedChannelBuilder.forAddress(analysisHost, analysisPort).usePlaintext().build();

        this.userService = UserServiceGrpc.newBlockingStub(userChannel);
        this.gameService = GameServiceGrpc.newBlockingStub(gameChannel);
        this.analysisService = AnalysisServiceGrpc.newBlockingStub(analysisChannel);

        log.info("[GRPC] Clients initialized: user={}:{}, game={}:{}, analysis={}:{}",
            userHost, userPort, gameHost, gamePort, analysisHost, analysisPort);
    }

    public UserServiceGrpc.UserServiceBlockingStub userService() {
        return userService;
    }

    public GameServiceGrpc.GameServiceBlockingStub gameService() {
        return gameService;
    }

    public AnalysisServiceGrpc.AnalysisServiceBlockingStub analysisService() {
        return analysisService;
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
