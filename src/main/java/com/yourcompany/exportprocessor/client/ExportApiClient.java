package com.yourcompany.exportprocessor.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Component
public class ExportApiClient {

    private final HttpClient httpClient;
    private final String baseUrl;
    private final Duration timeout;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    public ExportApiClient(
            HttpClient httpClient,
            @Value("${app.export-api.base-url}") String baseUrl,
            @Value("${app.export-api.timeout-seconds:300}") int timeoutSeconds,
            Retry exportApiRetry,
            CircuitBreaker exportApiCircuitBreaker) {

        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.retry = exportApiRetry;
        this.circuitBreaker = exportApiCircuitBreaker;
    }

    /**
     * Downloads export data with retry and circuit breaker protection.
     *
     * @param exportId The export identifier
     * @return InputStream of the export data (caller must close)
     */
    public InputStream downloadExport(String exportId) {
        Supplier<InputStream> decorated = Decorators
                .ofSupplier(() -> doDownload(exportId))
                .withRetry(retry)
                .withCircuitBreaker(circuitBreaker)
                .decorate();

        return decorated.get();
    }

    private InputStream doDownload(String exportId) {
        try {
            String url = baseUrl + "/exports/" + exportId + "/download";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .header("Accept", "application/octet-stream")
                    .GET()
                    .build();

            log.debug("Downloading export: url={}", url);

            HttpResponse<InputStream> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofInputStream()
            );

            if (response.statusCode() != 200) {
                throw new RuntimeException("Export API returned status: " + response.statusCode());
            }

            return response.body();

        } catch (IOException | InterruptedException e) {
            log.error("Failed to download export: exportId={}", exportId, e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Export download failed", e);
        }
    }
}
