package grpc;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcServer {
    private static final Logger log = LoggerFactory.getLogger(GrpcServer.class);
    private final Server server;

    public GrpcServer(int port, BindableService... services) {
        ServerBuilder<?> builder = ServerBuilder.forPort(port);
        for (BindableService service : services) {
            builder.addService(service);
        }
        this.server = builder.build();
    }

    public void start() throws Exception {
        server.start();
        log.info("[GRPC] Server started on port {}", server.getPort());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
            log.info("[GRPC] Server shut down");
        }));
    }

    public void blockUntilShutdown() throws InterruptedException {
        server.awaitTermination();
    }
}
