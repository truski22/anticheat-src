package http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpRequest.BodyPublishers;
import java.time.Duration;

public class MlServiceHttpClient {
    private static final String ML_TOKEN_ENV = "ML_SERVICE_TOKEN";
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).version(HttpClient.Version.HTTP_1_1).build();
    private final String bearerToken;

    public MlServiceHttpClient() {
        String token = System.getenv(ML_TOKEN_ENV);
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                "[ANALYSIS] FATAL: ML_SERVICE_TOKEN environment variable is not set."
            );
        }
        this.bearerToken = token;
    }

    public HttpResponse<String> sendHttpRequest(String uri, String moves) throws IOException, InterruptedException {
        String jsonBody = String.format(
                "{ \"moves\": \"%s\" }",
                moves.replace("\"", "\\\"")
        );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + bearerToken)
                .POST(BodyPublishers.ofString(jsonBody))
                .build();
        return client.send(request, BodyHandlers.ofString());
    }
}
