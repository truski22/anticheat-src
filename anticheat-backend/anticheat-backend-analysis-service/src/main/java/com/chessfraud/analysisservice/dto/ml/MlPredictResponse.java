package com.chessfraud.analysisservice.dto.ml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Inbound response body from ml-service. Bound directly by {@link org.springframework.web.client.RestClient}
 * (Jackson) instead of the original ad-hoc {@code JsonUtils.extractNestedField}/{@code extractIntList} parsing.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MlPredictResponse(
        Prediction white,
        Prediction black,
        @JsonProperty("data_white") List<Integer> dataWhite,
        @JsonProperty("data_black") List<Integer> dataBlack) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Prediction(String prediction) {
    }
}
