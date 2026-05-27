package websocket;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;

public class MessageEncoder implements Encoder.Text<Message> {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MessageEncoder.class);
    private Jsonb jsonb;
    @Override
    public String encode(Message message) throws EncodeException {
        return jsonb.toJson(message);
    }

    @Override
    public void init(EndpointConfig config) {
        jsonb = JsonbBuilder.create();
    }

    @Override
    public void destroy() {
        try {
            jsonb.close();
        } catch (Exception e) {
            log.error("Failed to close JSON-B instance: {}", e.getMessage(), e);
        }
    }
}
