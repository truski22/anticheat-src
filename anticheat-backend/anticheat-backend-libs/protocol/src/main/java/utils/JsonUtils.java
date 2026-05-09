package utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public final class JsonUtils {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JsonUtils.class);
    public static String extractField(String message,String property){
        String res=null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode rootNode = mapper.readTree(message);
            if (rootNode.has(property)) {
                res = rootNode.get(property).asText();
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON property '{}': {}", property, e.getMessage(), e);
        }
        return res;
    }
    public static boolean extractBooleanField(String message, String property){
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode rootNode = mapper.readTree(message);
            if (rootNode.has(property)) {
                return rootNode.get(property).asBoolean();
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse boolean property '{}': {}", property, e.getMessage(), e);
        }
        return false;
    }
    public static JsonNode extractNode(String message, String property) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode rootNode = mapper.readTree(message);
            return rootNode.get(property);
        } catch (JsonProcessingException e) {
            log.error("Failed to get JSON node '{}': {}", property, e.getMessage(), e);
        }
        return null;
    }
    public static List<Integer> extractIntList(String message, String property) {
        List<Integer> result = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode rootNode = mapper.readTree(message);
            if (rootNode.has(property) && rootNode.get(property).isArray()) {
                for (JsonNode node : rootNode.get(property)) {
                    result.add(node.asInt());
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse integer list property '{}': {}", property, e.getMessage(), e);
        }
        return result;
    }
}

