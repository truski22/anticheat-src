package config;

/**
 * Configuration holder for the analysis-service, populated from environment variables.
 * Use {@link #fromEnvironment()} to build an instance at startup.
 */
public class AnalysisServiceConfig {

    private final String chessInsightsUrl;
    private final String mlServiceToken;

    private AnalysisServiceConfig(String chessInsightsUrl, String mlServiceToken) {
        this.chessInsightsUrl = chessInsightsUrl;
        this.mlServiceToken   = mlServiceToken;
    }

    /**
     * Creates an {@code AnalysisServiceConfig} by reading the following environment variables:
     * <ul>
     *   <li>{@code CHESS_INSIGHTS_URL} – Full URL of the ML inference endpoint
     *       (default: {@code http://localhost:5002/predict})</li>
     *   <li>{@code ML_SERVICE_TOKEN}   – Bearer token used to authenticate with the ML service</li>
     * </ul>
     *
     * @return a fully populated {@code AnalysisServiceConfig}
     * @throws IllegalStateException if {@code ML_SERVICE_TOKEN} is not set
     */
    public static AnalysisServiceConfig fromEnvironment() {
        String url   = System.getenv().getOrDefault("CHESS_INSIGHTS_URL", "http://localhost:5002/predict");
        String token = System.getenv("ML_SERVICE_TOKEN");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "[ANALYSIS] FATAL: ML_SERVICE_TOKEN environment variable is not set.");
        }
        return new AnalysisServiceConfig(url, token);
    }

    /** @return full URL of the ML inference endpoint */
    public String getChessInsightsUrl() { return chessInsightsUrl; }

    /** @return Bearer token for authenticating with the ML service */
    public String getMlServiceToken() { return mlServiceToken; }
}
