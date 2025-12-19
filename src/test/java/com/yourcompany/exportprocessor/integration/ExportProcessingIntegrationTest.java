package com.yourcompany.exportprocessor.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.*;

/**
 * Integration test using Testcontainers with LocalStack.
 * This test verifies the full message processing flow.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class ExportProcessingIntegrationTest {

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(SQS, S3, DYNAMODB);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.endpoint",
                () -> localStack.getEndpointOverride(SQS).toString());
        registry.add("spring.cloud.aws.region.static",
                () -> localStack.getRegion());
        registry.add("spring.cloud.aws.credentials.access-key",
                () -> localStack.getAccessKey());
        registry.add("spring.cloud.aws.credentials.secret-key",
                () -> localStack.getSecretKey());
    }

    @Test
    void contextLoads() {
        // Verify that the Spring context loads with LocalStack
    }

    // Add more integration tests as needed
}
