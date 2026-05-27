import grpc.GameServiceGrpcImpl;
import grpc.GrpcServer;
import service.GamePersistenceService;
import utils.Database;
import utils.HealthServer;

public class GameServiceApplication {
    private static final int HEALTH_PORT = 9092;
    private static final int GRPC_PORT = 9091;

    public void startModule() throws Exception {
        HealthServer.start(HEALTH_PORT);

        Database database = new Database();
        GamePersistenceService gameService = new GamePersistenceService(database);

        GameServiceGrpcImpl grpcImpl = new GameServiceGrpcImpl(gameService);
        GrpcServer server = new GrpcServer(GRPC_PORT, grpcImpl);
        server.start();
        server.blockUntilShutdown();
    }

    public static void main(String[] args) throws Exception {
        new GameServiceApplication().startModule();
    }
}
