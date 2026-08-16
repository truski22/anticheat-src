package com.chessfraud.analysisservice.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration holder for the analysis-service.
 */
@ConfigurationProperties(prefix = "analysis-service")
public class AnalysisServiceConfig {

    private String chessInsightsUrl = "http://localhost:5002/predict";
    private String mlServiceToken;

    /**
     * Fails startup the same way the original {@code AnalysisServiceConfig.fromEnvironment()}
     * / {@code MlServiceHttpClient} constructors did: this service is useless without a
     * token to authenticate against ml-service.
     */
    @PostConstruct
    void validate() {
        if (mlServiceToken == null || mlServiceToken.isBlank()) {
            throw new IllegalStateException(
                    "[ANALYSIS] FATAL: ML_SERVICE_TOKEN environment variable is not set.");
        }
    }

    public String getChessInsightsUrl() {
        return chessInsightsUrl;
    }

    public void setChessInsightsUrl(String chessInsightsUrl) {
        this.chessInsightsUrl = chessInsightsUrl;
    }

    public String getMlServiceToken() {
        return mlServiceToken;
    }

    public void setMlServiceToken(String mlServiceToken) {
        this.mlServiceToken = mlServiceToken;
    }
}
