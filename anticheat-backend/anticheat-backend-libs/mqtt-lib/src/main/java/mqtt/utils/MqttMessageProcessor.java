package mqtt.utils;

import java.util.concurrent.LinkedBlockingQueue;

public class MqttMessageProcessor {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MqttMessageProcessor.class);
    private final LinkedBlockingQueue<String> messageQueue;

    public MqttMessageProcessor() {
        this.messageQueue = new LinkedBlockingQueue<>();
    }
    public void addMessage(String message) {
        try {
            messageQueue.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to add message to the queue: {}", e.getMessage(), e);
        }
    }
    public String consume() {
        try {
            return messageQueue.take(); // Blocking get to ensure thread-safety
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to retrieve message from the queue: {}", e.getMessage(), e);
            return null;
        }
    }
    public boolean isEmpty() {
        return messageQueue.isEmpty();
    }
    public int getQueueSize() {
        return messageQueue.size();
    }

    public LinkedBlockingQueue<String> getMessageQueue() {
        return messageQueue;
    }
}
