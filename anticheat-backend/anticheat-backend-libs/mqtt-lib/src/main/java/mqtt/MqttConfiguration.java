package mqtt;

import mqtt.exception.MqttException;
import mqtt.utils.Topic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class MqttConfiguration {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MqttConfiguration.class);
    private static String mqttId;
    private static int maxMessagesInQueue;
    private static Map<String, Topic> publicationsTopic= new HashMap<>();
    private static Map<String,Topic> subscriptionTopic=new HashMap<>();


    public static boolean loadConfiguration(String path) throws MqttException {
        boolean res=false;
        if(!isFile(path)){
            throw new MqttException("Invalid configuration path");
        }
        Properties properties = new Properties();
        try(FileInputStream fis = new FileInputStream(path)){
            properties.load(fis);
            loadProperties(properties);
            res=true;
        } catch (IOException e) {
            log.error("Failed to load MQTT configuration from {}: {}", path, e.getMessage(), e);
            res = false;
        }
        return res;
    }

    private static void loadProperties(Properties properties) {
        mqttId = properties.getProperty("mqttId");
        maxMessagesInQueue = Integer.valueOf(properties.getProperty("maxMessagesInQueue"));
        loadSubscriptions(properties);
        loadPublications(properties);
    }

    private static void loadPublications(Properties properties) {
        int numPublication = Integer.valueOf(properties.getProperty("num_publications"));
        String publication = "publication_";
        getTopicInfo(properties, numPublication, publication, publicationsTopic);
    }
    private static void loadSubscriptions(Properties properties) {
        int numSubscription = Integer.valueOf(properties.getProperty("num_subscriptions"));
        String subscription = "subscription_";
        getTopicInfo(properties, numSubscription, subscription, subscriptionTopic);
    }
    private static void getTopicInfo(Properties properties, int numPublication, String publication, Map<String, Topic> mapTopic) {
        for (int i = 1; i < numPublication+1; i++) {
            String topic = properties.getProperty(publication + i + "_topic", "");
            String url = resolveEnvVars(properties.getProperty(publication + i + "_URL_factory", ""));
            String user = resolveEnvVars(properties.getProperty(publication + i + "_user", ""));
            String password = resolveEnvVars(properties.getProperty(publication + i + "_password", ""));
            int timeTryReconnect = Integer.parseInt(properties.getProperty(publication + i + "_timeTryReconnect", "0"));

            String messagesStr = properties.getProperty(publication + i + "_messages", "");
            List<String> messages = getMessages(messagesStr);
            Topic topicInfo = new Topic(topic,url,user,password,timeTryReconnect,messages);
            mapTopic.put(topic,topicInfo);
        }
    }

    /** Replaces ${ENV_VAR} tokens with their environment variable values. */
    private static String resolveEnvVars(String value) {
        if (value == null || !value.contains("${")) return value;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\$\\{([^}]+)\\}").matcher(value);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String envValue = System.getenv(m.group(1));
            m.appendReplacement(sb, envValue != null ? envValue : m.group(0));
        }
        m.appendTail(sb);
        return sb.toString();
    }
    private static List<String> getMessages(String messages){
        List<String> res = new ArrayList<String>();
        if(messages!=null){
            String[]messagesArray = messages.split(",");
            res = Arrays.asList(messagesArray);
        }
        return res;
    }

    private static boolean isFile(String path){
        boolean isFile=true;
        File file = new File(path);
        if(!file.isFile()){
            isFile=false;
        }
        return isFile;
    }

    public static String getMqttId() {
        return mqttId;
    }

    public static void setMqttId(String mqttId) {
        MqttConfiguration.mqttId = mqttId;
    }

    public static int getMaxMessagesInQueue() {
        return maxMessagesInQueue;
    }

    public static void setMaxMessagesInQueue(int maxMessagesInQueue) {
        MqttConfiguration.maxMessagesInQueue = maxMessagesInQueue;
    }

    public static Map<String, Topic> getPublicationsTopic() {
        return publicationsTopic;
    }

    public static void setPublicationsTopic(Map<String, Topic> publicationsTopic) {
        MqttConfiguration.publicationsTopic = publicationsTopic;
    }

    public static Map<String, Topic> getSubscriptionTopic() {
        return subscriptionTopic;
    }

    public static void setSubscriptionTopic(Map<String, Topic> subscriptionTopic) {
        MqttConfiguration.subscriptionTopic = subscriptionTopic;
    }
}