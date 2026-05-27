import config.AnalysisServiceConfig;
import grpc.AnalysisServiceGrpcImpl;
import grpc.GrpcServer;
import service.GameAnalysisService;
import utils.HealthServer;

public class AnalysisServiceApplication {
    private static final int HEALTH_PORT = 9093;
    private static final int GRPC_PORT = 9092;

    public void startModule() throws Exception {
        HealthServer.start(HEALTH_PORT);

        AnalysisServiceConfig config = AnalysisServiceConfig.fromEnvironment();
        GameAnalysisService analysisService = new GameAnalysisService(config);

        AnalysisServiceGrpcImpl grpcImpl = new AnalysisServiceGrpcImpl(analysisService);
        GrpcServer server = new GrpcServer(GRPC_PORT, grpcImpl);
        server.start();
        server.blockUntilShutdown();
    }

    public static void main(String[] args) throws Exception {
        new AnalysisServiceApplication().startModule();
    }
}
