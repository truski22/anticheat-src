package mqtt.Subscriber;

import org.eclipse.paho.client.mqttv3.MqttException;
import mqtt.utils.MqttMessageProcessor;
import mqtt.utils.Topic;

import java.util.HashMap;
import java.util.Map;

public class SubscribersManager {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SubscribersManager.class);
    private Map<String,Subscriber> subcriberMap;
    private Map<String, Topic> subscriberTopic;
    private MqttMessageProcessor messageProcessor;
    public SubscribersManager(Map<String,Topic> subscriberTopic, MqttMessageProcessor messageProcessor) {
        this.subcriberMap = new HashMap<String, Subscriber>();
        this.subscriberTopic = subscriberTopic;
        this.messageProcessor = messageProcessor;
        initSubscribers();
    }

    private void initSubscribers() {
        for(Map.Entry<String,Topic> entry:subscriberTopic.entrySet()){
            Subscriber subscriber = new Subscriber(entry.getValue(),messageProcessor);
            subcriberMap.put(entry.getKey(), subscriber);
        }
    }

    public void stop(){
        for(Subscriber subscriber:subcriberMap.values()){
            try {
                subscriber.stop();
            } catch (MqttException e) {
                log.error("Error trying to stop a subscriber", e);
            }
        }
    }
}
