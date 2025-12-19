package com.yourcompany.exportprocessor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

import java.net.URI;

@Configuration
public class S3Config {

    @Value("${spring.cloud.aws.region.static:us-east-1}")
    private String awsRegion;

    @Value("${spring.cloud.aws.endpoint:#{null}}")
    private String awsEndpoint;

    @Value("${app.processing.multipart-threshold-mb:100}")
    private long multipartThresholdMb;

    /**
     * S3 Async Client optimized for large file transfers.
     * Uses CRT client for high throughput when available.
     */
    @Bean
    public S3AsyncClient s3AsyncClient(AwsCredentialsProvider awsCredentialsProvider) {
        var builder = S3AsyncClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(awsCredentialsProvider);

        if (awsEndpoint != null && !awsEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(awsEndpoint))
                   .forcePathStyle(true);  // Required for LocalStack
        }

        return builder.build();
    }

    /**
     * S3 Transfer Manager for automatic multipart uploads.
     * Handles files larger than the threshold automatically.
     */
    @Bean
    public S3TransferManager s3TransferManager(S3AsyncClient s3AsyncClient) {
        return S3TransferManager.builder()
                .s3Client(s3AsyncClient)
                .build();
    }
}
