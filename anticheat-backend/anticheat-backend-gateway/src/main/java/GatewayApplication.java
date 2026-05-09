import logic.GatewayMessageRouter;

public class GatewayApplication {
    private static final String MQTT_CONFIG_PATH = "chessfraud-libs/protocol/src/main/configuration/gateway/mqtt.properties";

    public void startModule(){
        new GatewayMessageRouter(MQTT_CONFIG_PATH);
    }

    public static void main(String[] args){
        new GatewayApplication().startModule();
    }
}
