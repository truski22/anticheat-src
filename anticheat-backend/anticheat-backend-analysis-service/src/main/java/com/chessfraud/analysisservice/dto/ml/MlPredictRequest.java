package com.chessfraud.analysisservice.dto.ml;

/**
 * Outbound request body sent to ml-service. Serialized to JSON by {@link org.springframework.web.client.RestClient}
 * (Jackson) instead of the original hand-formatted string, which only escaped double quotes
 * and broke on move strings containing backslashes or control characters.
 */
public record MlPredictRequest(String moves) {
}
