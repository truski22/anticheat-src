import logic.GatewayMessageRouter;

public class GatewayApplication {
    public void startModule() {
        new GatewayMessageRouter();
    }

    public static void main(String[] args) {
        new GatewayApplication().startModule();
    }
}
