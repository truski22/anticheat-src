package mqtt.Publisher;

import org.eclipse.paho.client.mqttv3.MqttException;
import mqtt.utils.Topic;

import java.util.HashMap;
import java.util.Map;

public class PublishersManager {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PublishersManager.class);
    private Map<String,Publisher> publishersMap;
    private Map<String,Topic> publishersTopic;
    public PublishersManager(Map<String,Topic> publishersTopic) {
        this.publishersMap = new HashMap<String, Publisher>();
        this.publishersTopic = publishersTopic;
        initPublishers();
    }

    private void initPublishers() {
        for(Map.Entry<String,Topic> entry:publishersTopic.entrySet()){
            Publisher publisher = new Publisher(entry.getValue());
            publishersMap.put(entry.getKey(), publisher);
        }
    }

    public void publishMessage(Object message){
        if(message !=null){
            String messageType = message.getClass().getSimpleName();
            for (Map.Entry<String, Publisher> entry : publishersMap.entrySet()) {
                Publisher publisher = entry.getValue();
                if (publisher.getAcceptedMessages().contains(messageType)) {
                    publisher.publish(message);
                }
            }
        }else{
            log.error("Message is null");
        }
    }
    public void stop(){
        for(Publisher p:publishersMap.values()){
            try {
                p.stop();
            } catch (MqttException e) {
                log.error("Error trying to stop a publisher", e);
            }
        }
    }
}
