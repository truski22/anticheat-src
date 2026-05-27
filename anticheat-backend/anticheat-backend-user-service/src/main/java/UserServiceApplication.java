import config.UserServiceConfig;
import grpc.GrpcServer;
import grpc.UserServiceGrpcImpl;
import service.EmailNotificationService;
import service.UserAccountService;
import utils.Database;
import utils.HealthServer;

public class UserServiceApplication {
    private static final int HEALTH_PORT = 9091;
    private static final int GRPC_PORT = 9090;

    public void startModule() throws Exception {
        HealthServer.start(HEALTH_PORT);

        Database database = new Database();
        UserServiceConfig config = UserServiceConfig.fromEnvironment();
        UserAccountService accountService = new UserAccountService(database);
        EmailNotificationService emailService = new EmailNotificationService(config);

        UserServiceGrpcImpl grpcImpl = new UserServiceGrpcImpl(accountService, emailService);
        GrpcServer server = new GrpcServer(GRPC_PORT, grpcImpl);
        server.start();
        server.blockUntilShutdown();
    }

    public static void main(String[] args) throws Exception {
        new UserServiceApplication().startModule();
    }
}
