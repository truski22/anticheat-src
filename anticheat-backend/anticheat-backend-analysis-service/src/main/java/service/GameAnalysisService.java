package service;

import config.AnalysisServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.JsonUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Calls the external ML inference service to classify a chess game as fair or fraudulent.
 * Results are cached in-memory keyed by a SHA-256 hash of the normalised move string.
 */
public class GameAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(GameAnalysisService.class);
    private static final int MAX_CACHE_SIZE = 500;

    private final AnalysisServiceConfig config;
    private final HttpClient httpClient;
    private final Map<String, AnalysisResult> cache;

    public GameAnalysisService(AnalysisServiceConfig config) {
        this.config     = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        // LRU cache: removes eldest entry when size exceeds MAX_CACHE_SIZE
        this.cache = new LinkedHashMap<>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, AnalysisResult> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        };
    }

    /**
     * Sends the move list to the ML service and returns a structured {@link AnalysisResult}.
     * Cached results are returned immediately without calling the ML service.
     */
    public AnalysisResult analyze(String moves) throws IOException, InterruptedException {
        String cacheKey = hashMoves(moves);

        synchronized (cache) {
            AnalysisResult cached = cache.get(cacheKey);
            if (cached != null) {
                log.info("[ANALYSIS] Cache hit for moves hash {}", cacheKey.substring(0, 12));
                return cached;
            }
        }

        String jsonBody = String.format(
                "{ \"moves\": \"%s\" }",
                moves.replace("\"", "\\\""));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getChessInsightsUrl()))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.getMlServiceToken())
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.debug("[ANALYSIS] ML service responded with status {}", httpResponse.statusCode());

        AnalysisResult result = parseResponse(httpResponse.body());

        synchronized (cache) {
            cache.put(cacheKey, result);
        }

        return result;
    }

    private AnalysisResult parseResponse(String body) {
        boolean legal          = "0".equals(JsonUtils.extractField(body, "prediction"));
        List<Integer> white    = JsonUtils.extractIntList(body, "data_white");
        List<Integer> black    = JsonUtils.extractIntList(body, "data_black");
        return new AnalysisResult(legal, white, black);
    }

    private static String hashMoves(String moves) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(moves.strip().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available in JDK
            throw new RuntimeException(e);
        }
    }
}
