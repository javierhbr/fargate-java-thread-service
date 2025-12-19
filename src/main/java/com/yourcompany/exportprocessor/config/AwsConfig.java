package com.yourcompany.exportprocessor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class AwsConfig {

    @Value("${spring.cloud.aws.region.static:us-east-1}")
    private String awsRegion;

    @Value("${spring.cloud.aws.endpoint:#{null}}")
    private String awsEndpoint;

    @Value("${spring.cloud.aws.credentials.access-key:#{null}}")
    private String accessKey;

    @Value("${spring.cloud.aws.credentials.secret-key:#{null}}")
    private String secretKey;

    /**
     * AWS Credentials Provider that supports both Spring Cloud AWS config and default provider chain.
     * Uses static credentials if configured, otherwise falls back to default chain.
     */
    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        }
        return DefaultCredentialsProvider.create();
    }

    /**
     * Java 21 HttpClient for Export API calls.
     * Configured with virtual thread compatibility.
     */
    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * SQS Client for visibility timeout extensions (heartbeat).
     * Note: Message consumption uses Spring Cloud AWS @SqsListener
     */
    @Bean
    public SqsClient sqsClient(AwsCredentialsProvider awsCredentialsProvider) {
        var builder = SqsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(awsCredentialsProvider);

        if (awsEndpoint != null && !awsEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(awsEndpoint));
        }

        return builder.build();
    }
}
