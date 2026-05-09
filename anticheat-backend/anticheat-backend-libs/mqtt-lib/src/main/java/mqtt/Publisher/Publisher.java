package mqtt.Publisher;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import mqtt.utils.Topic;

public class Publisher {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Publisher.class);
    private String brokerUrl;
    private String user;
    private String password;
    private int timeTryReconnect;
    private String topic;
    private List<String> acceptedMessages;
    private MqttClient client;
    private ObjectMapper objectMapper;

    public Publisher(Topic topic)  {
        this.brokerUrl = topic.getUrlFactory();
        this.user = topic.getUser();
        this.password = topic.getPassword();
        this.timeTryReconnect = topic.getTimeTryReconnect();
        this.topic = topic.getTopic();
        this.acceptedMessages = topic.getMessages();
        this.objectMapper = new ObjectMapper();
        try {
            this.client = new MqttClient(this.brokerUrl,MqttClient.generateClientId(),new MemoryPersistence());
        } catch (MqttException e) {
            log.error("Failed to create MQTT client: {}", e.getMessage(), e);
        }
        connect();
    }

    private void connect() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        if(user!=null && password!=null){
            options.setUserName(user);
            options.setPassword(password.toCharArray());
        }
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(timeTryReconnect);
        try {
            client.connect(options);
        } catch (MqttException e) {
            log.error("Failed to connect MQTT client: {}", e.getMessage(), e);
        }
    }
    public void publish(Object message){
        try {
            String jsonMessage=objectMapper.writeValueAsString(message);
            MqttMessage mqttMessage = new MqttMessage(jsonMessage.getBytes());
            mqttMessage.setQos(1);
            client.publish(topic,mqttMessage);
        } catch (JsonProcessingException | MqttException e) {
            log.error("Failed to publish MQTT message: {}", e.getMessage(), e);
        }
    }
    public void stop() throws MqttException {
        if(client.isConnected()){
            client.disconnect();
        }
    }

    public List<String> getAcceptedMessages() {
        return acceptedMessages;
    }
}
