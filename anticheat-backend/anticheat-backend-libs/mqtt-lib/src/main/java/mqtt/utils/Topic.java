package mqtt.utils;

import java.util.List;

public class Topic {
    private String topic;
    private String urlFactory;
    private String user;
    private String password;
    private int timeTryReconnect;
    private List<String> messages;

    public Topic(String topic, String urlFactory, String user, String password, int timeTryReconnect, List<String> messages) {
        this.topic = topic;
        this.urlFactory = urlFactory;
        this.user = user;
        this.password = password;
        this.timeTryReconnect = timeTryReconnect;
        this.messages = messages;
    }

    public String getUrlFactory() {
        return urlFactory;
    }

    public void setUrlFactory(String urlFactory) {
        this.urlFactory = urlFactory;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getTimeTryReconnect() {
        return timeTryReconnect;
    }

    public void setTimeTryReconnect(int timeTryReconnect) {
        this.timeTryReconnect = timeTryReconnect;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
