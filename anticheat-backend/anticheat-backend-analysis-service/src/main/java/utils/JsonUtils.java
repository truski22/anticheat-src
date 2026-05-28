package utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class JsonUtils {
    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtils() {
    }

    public static String extractField(String message, String property) {
        try {
            JsonNode rootNode = MAPPER.readTree(message);
            if (rootNode.has(property)) {
                return rootNode.get(property).asText();
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON property '{}': {}", property, e.getMessage(), e);
        }
        return null;
    }

    public static String extractNestedField(String message, String parent, String child) {
        try {
            JsonNode rootNode = MAPPER.readTree(message);
            JsonNode parentNode = rootNode.get(parent);
            if (parentNode != null && parentNode.has(child)) {
                return parentNode.get(child).asText();
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse nested JSON property '{}.{}': {}", parent, child, e.getMessage(), e);
        }
        return null;
    }

    public static boolean extractBooleanField(String message, String property) {
        try {
            JsonNode rootNode = MAPPER.readTree(message);
            if (rootNode.has(property)) {
                return rootNode.get(property).asBoolean();
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse boolean property '{}': {}", property, e.getMessage(), e);
        }
        return false;
    }

    public static JsonNode extractNode(String message, String property) {
        try {
            JsonNode rootNode = MAPPER.readTree(message);
            return rootNode.get(property);
        } catch (JsonProcessingException e) {
            log.error("Failed to get JSON node '{}': {}", property, e.getMessage(), e);
        }
        return null;
    }

    public static List<Integer> extractIntList(String message, String property) {
        List<Integer> result = new ArrayList<>();
        try {
            JsonNode rootNode = MAPPER.readTree(message);
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
