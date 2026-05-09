package mqtt;

import java.io.Serializable;

public class MqttMessage implements Serializable {
    private String id;
    private long timestamp;
    private Object message;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Object getMessage() {
        return message;
    }

    public void setMessage(Object message) {
        this.message = message;
    }
}
