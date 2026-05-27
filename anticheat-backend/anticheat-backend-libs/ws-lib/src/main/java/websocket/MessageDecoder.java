package websocket;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.websocket.Decoder;
import jakarta.websocket.EndpointConfig;

public class MessageDecoder implements Decoder.Text<Message> {
    private Jsonb jsonb;

    @Override
    public Message decode(String s) {
        return jsonb.fromJson(s, Message.class);
    }

    @Override
    public boolean willDecode(String s) {
        return s != null && !s.trim().isEmpty();
    }

    @Override
    public void init(EndpointConfig config) {
        jsonb = JsonbBuilder.create();
    }

    @Override
    public void destroy() {
        try {
            jsonb.close();
        } catch (Exception ignored) {}
    }
}
