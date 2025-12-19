package com.yourcompany.exportprocessor.config;

import com.yourcompany.exportprocessor.model.JobTracking;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

@Configuration
public class DynamoDbConfig {

    @Value("${spring.cloud.aws.region.static:us-east-1}")
    private String awsRegion;

    @Value("${spring.cloud.aws.endpoint:#{null}}")
    private String awsEndpoint;

    @Value("${app.dynamodb.job-tracking-table}")
    private String jobTrackingTableName;

    @Bean
    public DynamoDbClient dynamoDbClient(AwsCredentialsProvider awsCredentialsProvider) {
        var builder = DynamoDbClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(awsCredentialsProvider);

        if (awsEndpoint != null && !awsEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(awsEndpoint));
        }

        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Bean
    public DynamoDbTable<JobTracking> jobTrackingTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(jobTrackingTableName, TableSchema.fromBean(JobTracking.class));
    }
}
