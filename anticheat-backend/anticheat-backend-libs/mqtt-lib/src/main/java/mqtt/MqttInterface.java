package mqtt;

import mqtt.Publisher.PublishersManager;
import mqtt.Subscriber.SubscribersManager;
import mqtt.exception.MqttException;
import mqtt.utils.MqttMessageProcessor;

import java.util.List;

public class MqttInterface {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MqttInterface.class);
    private PublishersManager publishersManager;
    private SubscribersManager subscribersManager;
    private MqttMessageProcessor mqttMessageProcessor;
    public MqttInterface(String path) throws MqttException {
        if(path!=null){
            if(MqttConfiguration.loadConfiguration(path)){
                publishersManager = new PublishersManager(MqttConfiguration.getPublicationsTopic());
                mqttMessageProcessor = new MqttMessageProcessor();
                subscribersManager = new SubscribersManager(MqttConfiguration.getSubscriptionTopic(),mqttMessageProcessor);
            }else{
                log.error("Wrong configuration");
            }
        }
    }

    public void publishMessage(Object message){
        if( message != null && publishersManager!=null){
            publishersManager.publishMessage(message);
        }else{
            log.error("Error in MqttInterface: message or publishersManager is null");
        }
    }
    public void stopInterface(){
        publishersManager.stop();
        subscribersManager.stop();
    }

    public MqttMessageProcessor getMqttMessageProcessor() {
        return mqttMessageProcessor;
    }

    /**
     * Creates an {@link MqttMessageRouter} pre-wired to this interface's message queue.
     * Call {@link MqttMessageRouter#start()} to begin consuming messages.
     *
     * @param handlers handlers to register; each must have a unique
     *                 {@link MqttPayloadHandler#getHandledMessageType()}
     * @return a ready-to-start router
     */
    public MqttMessageRouter createRouter(List<MqttPayloadHandler> handlers) {
        return new MqttMessageRouter(mqttMessageProcessor, handlers);
    }
}
