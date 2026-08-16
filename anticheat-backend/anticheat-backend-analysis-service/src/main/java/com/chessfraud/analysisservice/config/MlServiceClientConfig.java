package com.chessfraud.analysisservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Builds the {@link RestClient} used by {@link com.chessfraud.analysisservice.service.analysis.GameAnalysisService}
 * to call ml-service. Same timeouts (5s connect / 10s read) as the original hand-rolled
 * {@code java.net.http.HttpClient} setup.
 */
@Configuration
public class MlServiceClientConfig {

    @Bean
    public RestClient mlServiceRestClient(AnalysisServiceConfig config) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(10_000);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getMlServiceToken())
                .build();
    }
}
