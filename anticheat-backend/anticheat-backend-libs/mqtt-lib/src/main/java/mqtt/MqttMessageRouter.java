package mqtt;

import mqtt.utils.MqttMessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.JsonUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Routes incoming MQTT messages to the appropriate {@link MqttPayloadHandler}.
 * <p>
 * Runs as a single daemon thread that blocks on the {@link MqttMessageProcessor}
 * queue, extracts the {@code messageType} JSON field from each message, and
 * dispatches to the registered handler.  Replaces manual {@code if-else} chains
 * and the per-service {@code *MqttConsumer} thread classes.
 *
 * <pre>{@code
 * MqttMessageRouter router = mqttInterface.createRouter(List.of(
 *     new LoginRequestHandler(accountService, mqttInterface),
 *     new RegisterRequestHandler(accountService, mqttInterface)
 * ));
 * router.start();
 * }</pre>
 */
public class MqttMessageRouter extends Thread {

    private static final Logger log = LoggerFactory.getLogger(MqttMessageRouter.class);

    private final MqttMessageProcessor processor;
    private final Map<String, MqttPayloadHandler> handlers;
    private volatile boolean running = true;

    /**
     * @param processor queue from which raw JSON messages are consumed
     * @param handlers  list of handlers; each is registered by
     *                  {@link MqttPayloadHandler#getHandledMessageType()}
     * @throws IllegalArgumentException if two handlers declare the same message type
     */
    public MqttMessageRouter(MqttMessageProcessor processor, List<MqttPayloadHandler> handlers) {
        super("mqtt-message-router");
        setDaemon(true);
        this.processor = processor;
        this.handlers  = buildHandlerMap(handlers);
    }

    private static Map<String, MqttPayloadHandler> buildHandlerMap(List<MqttPayloadHandler> list) {
        Map<String, MqttPayloadHandler> map = new HashMap<>();
        for (MqttPayloadHandler h : list) {
            String type = h.getHandledMessageType();
            if (map.containsKey(type)) {
                throw new IllegalArgumentException(
                        "Duplicate MqttPayloadHandler for messageType: " + type);
            }
            map.put(type, h);
        }
        return map;
    }

    @Override
    public void run() {
        log.info("[MQTT-ROUTER] Started — registered types: {}", handlers.keySet());
        while (running) {
            String message = processor.consume();
            if (message != null) {
                route(message);
            }
        }
        log.info("[MQTT-ROUTER] Stopped");
    }

    private void route(String jsonPayload) {
        String messageType = JsonUtils.extractField(jsonPayload, "messageType");
        if (messageType == null) {
            log.warn("[MQTT-ROUTER] Received message without 'messageType' field — discarding");
            return;
        }
        MqttPayloadHandler handler = handlers.get(messageType);
        if (handler != null) {
            handler.handle(jsonPayload);
        } else {
            log.warn("[MQTT-ROUTER] No handler registered for messageType: {}", messageType);
        }
    }

    /** Signals the router thread to stop after processing the current message. */
    public void stopRouter() {
        running = false;
        interrupt();
    }
}
