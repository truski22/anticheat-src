package mqtt.Subscriber;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import mqtt.utils.MqttMessageProcessor;
import mqtt.utils.Topic;
import utils.JsonUtils;

import java.util.List;

public class Subscriber {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Subscriber.class);
    private MqttClient client;
    private String brokerUrl;
    private String user;
    private String password;
    private String topic;
    private int timeTryReconnect;
    private List<String> acceptedMessages;
    private MqttMessageProcessor messageProcessor;
    public Subscriber(Topic topic, MqttMessageProcessor messageProcessor)  {
        this.brokerUrl = topic.getUrlFactory();
        this.user = topic.getUser();
        this.password = topic.getPassword();
        this.topic = topic.getTopic();
        this.timeTryReconnect = topic.getTimeTryReconnect();
        this.acceptedMessages = topic.getMessages();
        this.messageProcessor = messageProcessor;
        try {
            this.client = new MqttClient(brokerUrl, MqttClient.generateClientId(),new MemoryPersistence());
        } catch (MqttException e) {
            log.error("Failed to create MQTT client: {}", e.getMessage(), e);
        }
        connectAndSubscribe();
    }

    private void connectAndSubscribe()  {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        if (user != null && password != null) {
            options.setUserName(user);
            options.setPassword(password.toCharArray());
        }
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(timeTryReconnect);
        try {
            client.connect(options);
            client.subscribe(topic, (t, message) -> {
                String receivedMessage = new String(message.getPayload());
                String messageType= JsonUtils.extractField(receivedMessage,"messageType");
                if (acceptedMessages.contains(messageType)) {
                    messageProcessor.addMessage(receivedMessage);
                }
            });
        } catch (MqttException e) {
            log.error("Failed to connect or subscribe: {}", e.getMessage(), e);
        }
    }

    public void stop()throws MqttException {
        if (client.isConnected()) {
            client.disconnect();
        }
    }
}

