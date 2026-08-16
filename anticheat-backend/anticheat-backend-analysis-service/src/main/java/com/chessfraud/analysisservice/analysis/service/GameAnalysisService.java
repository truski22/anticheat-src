package com.chessfraud.analysisservice.analysis.service;

import com.chessfraud.analysisservice.analysis.dto.AnalysisResult;
import com.chessfraud.analysisservice.analysis.dto.MlPredictRequest;
import com.chessfraud.analysisservice.analysis.dto.MlPredictResponse;
import com.chessfraud.analysisservice.config.AnalysisServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calls the external ML inference service to classify a chess game as fair or fraudulent.
 * Results are cached in-memory keyed by a SHA-256 hash of the normalised move string.
 */
@Service
public class GameAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(GameAnalysisService.class);
    private static final int MAX_CACHE_SIZE = 500;
    private static final String LEGAL_PREDICTION = "0";

    private final AnalysisServiceConfig config;
    private final RestClient restClient;
    private final Map<String, AnalysisResult> cache;

    public GameAnalysisService(AnalysisServiceConfig config, RestClient mlServiceRestClient) {
        this.config = config;
        this.restClient = mlServiceRestClient;
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
    public AnalysisResult analyze(String moves) {
        String cacheKey = hashMoves(moves);

        synchronized (cache) {
            AnalysisResult cached = cache.get(cacheKey);
            if (cached != null) {
                log.info("[ANALYSIS] Cache hit for moves hash {}", cacheKey.substring(0, 12));
                return cached;
            }
        }

        MlPredictResponse response = restClient.post()
                .uri(config.getChessInsightsUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MlPredictRequest(moves))
                .retrieve()
                .body(MlPredictResponse.class);

        AnalysisResult result = toAnalysisResult(response);

        synchronized (cache) {
            cache.put(cacheKey, result);
        }

        return result;
    }

    private AnalysisResult toAnalysisResult(MlPredictResponse response) {
        boolean whiteLegal = response != null && response.white() != null
                && LEGAL_PREDICTION.equals(response.white().prediction());
        boolean blackLegal = response != null && response.black() != null
                && LEGAL_PREDICTION.equals(response.black().prediction());
        List<Integer> dataWhite = response != null && response.dataWhite() != null ? response.dataWhite() : List.of();
        List<Integer> dataBlack = response != null && response.dataBlack() != null ? response.dataBlack() : List.of();
        return new AnalysisResult(whiteLegal, blackLegal, dataWhite, dataBlack);
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
