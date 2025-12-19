package com.yourcompany.exportprocessor.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class HeartbeatService {

    private final SqsClient sqsClient;
    private final String queueUrl;
    private final int heartbeatIntervalSeconds;
    private final int visibilityTimeoutSeconds;
    private final ScheduledExecutorService scheduler;

    public HeartbeatService(
            SqsClient sqsClient,
            @Value("${app.sqs.export-queue}") String queueName,
            @Value("${app.processing.heartbeat-interval-seconds:120}") int heartbeatIntervalSeconds,
            @Value("${spring.cloud.aws.region.static:us-east-1}") String region) {

        this.sqsClient = sqsClient;
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
        this.visibilityTimeoutSeconds = heartbeatIntervalSeconds + 60; // Buffer

        // Get queue URL
        this.queueUrl = sqsClient.getQueueUrl(r -> r.queueName(queueName)).queueUrl();

        // Use platform thread for scheduling (not virtual threads)
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "heartbeat-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Starts a heartbeat that extends message visibility timeout periodically.
     *
     * @param receiptHandle SQS message receipt handle
     * @return ScheduledFuture that can be cancelled when processing completes
     */
    public ScheduledFuture<?> startHeartbeat(String receiptHandle) {
        return scheduler.scheduleAtFixedRate(
                () -> extendVisibility(receiptHandle),
                heartbeatIntervalSeconds,
                heartbeatIntervalSeconds,
                TimeUnit.SECONDS
        );
    }

    private void extendVisibility(String receiptHandle) {
        try {
            sqsClient.changeMessageVisibility(ChangeMessageVisibilityRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(receiptHandle)
                    .visibilityTimeout(visibilityTimeoutSeconds)
                    .build());

            log.debug("Extended message visibility by {} seconds", visibilityTimeoutSeconds);

        } catch (Exception e) {
            log.warn("Failed to extend message visibility: {}", e.getMessage());
        }
    }

    /**
     * Releases message back to queue immediately (visibility = 0).
     * Use when gracefully shutting down without completing the job.
     */
    public void releaseMessage(String receiptHandle) {
        try {
            sqsClient.changeMessageVisibility(ChangeMessageVisibilityRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(receiptHandle)
                    .visibilityTimeout(0)
                    .build());

            log.info("Released message back to queue");

        } catch (Exception e) {
            log.warn("Failed to release message: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
