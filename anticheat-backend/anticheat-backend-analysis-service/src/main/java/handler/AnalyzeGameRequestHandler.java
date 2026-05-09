package handler;

import mqtt.MqttPayloadHandler;

import mqtt.AnalyzeGameResponse;
import mqtt.MqttInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.AnalysisResult;
import service.GameAnalysisService;
import utils.JsonUtils;

import java.io.IOException;

/**
 * Handles {@code AnalyzeGame} MQTT messages.
 * Delegates the ML inference to {@link GameAnalysisService} and publishes an
 * {@link AnalyzeGameResponse} containing the legality verdict and per-colour evaluation data.
 */
public class AnalyzeGameRequestHandler implements MqttPayloadHandler {

    private static final Logger log = LoggerFactory.getLogger(AnalyzeGameRequestHandler.class);

    private final GameAnalysisService analysisService;
    private final MqttInterface mqttInterface;

    /**
     * @param analysisService service that calls the ML inference endpoint
     * @param mqttInterface   used to publish the response message
     */
    public AnalyzeGameRequestHandler(GameAnalysisService analysisService, MqttInterface mqttInterface) {
        this.analysisService = analysisService;
        this.mqttInterface   = mqttInterface;
    }

    /**
     * Parses the incoming MQTT JSON payload, runs the ML analysis, and publishes an
     * {@link AnalyzeGameResponse}.
     * <p>Does nothing if any required field ({@code user}, {@code moves}) is missing.</p>
     *
     * @param mqttMessage raw JSON string received from the MQTT broker
     */
    @Override
    public String getHandledMessageType() { return "AnalyzeGame"; }

    @Override

    public void handle(String mqttMessage) {
        String username = JsonUtils.extractField(mqttMessage, "user");
        String moves    = JsonUtils.extractField(mqttMessage, "moves");

        if (username == null || moves == null) {
            log.warn("[AnalyzeGameRequestHandler] Missing required fields in AnalyzeGame message");
            return;
        }

        try {
            AnalysisResult result = analysisService.analyze(moves);

            AnalyzeGameResponse response = new AnalyzeGameResponse();
            response.setUser(username);
            response.setMessageType("AnalyzeGameResponse");
            response.setLegal(result.isLegal());
            response.setWhite(result.getDataWhite());
            response.setBlack(result.getDataBlack());

            mqttInterface.publishMessage(response);
        } catch (IOException | InterruptedException e) {
            log.error("[AnalyzeGameRequestHandler] Error calling ML service for user '{}': {}",
                    username, e.getMessage(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }
}


