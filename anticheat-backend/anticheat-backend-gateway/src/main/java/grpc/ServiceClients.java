package grpc;

import com.chessfraud.grpc.analysis.AnalysisServiceGrpc;
import com.chessfraud.grpc.game.GameServiceGrpc;
import com.chessfraud.grpc.user.UserServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceClients {
    private static final Logger log = LoggerFactory.getLogger(ServiceClients.class);

    private final UserServiceGrpc.UserServiceBlockingStub userService;
    private final GameServiceGrpc.GameServiceBlockingStub gameService;
    private final AnalysisServiceGrpc.AnalysisServiceBlockingStub analysisService;

    public ServiceClients() {
        String userHost = System.getenv().getOrDefault("USER_SERVICE_HOST", "user-service");
        String userPort = System.getenv().getOrDefault("USER_SERVICE_GRPC_PORT", "9090");
        String gameHost = System.getenv().getOrDefault("GAME_SERVICE_HOST", "game-service");
        String gamePort = System.getenv().getOrDefault("GAME_SERVICE_GRPC_PORT", "9091");
        String analysisHost = System.getenv().getOrDefault("ANALYSIS_SERVICE_HOST", "analysis-service");
        String analysisPort = System.getenv().getOrDefault("ANALYSIS_SERVICE_GRPC_PORT", "9092");

        ManagedChannel userChannel = ManagedChannelBuilder
            .forAddress(userHost, Integer.parseInt(userPort))
            .usePlaintext()
            .build();
        ManagedChannel gameChannel = ManagedChannelBuilder
            .forAddress(gameHost, Integer.parseInt(gamePort))
            .usePlaintext()
            .build();
        ManagedChannel analysisChannel = ManagedChannelBuilder
            .forAddress(analysisHost, Integer.parseInt(analysisPort))
            .usePlaintext()
            .build();

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
}
