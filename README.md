# SQS Export Processing Service

A Spring Boot 3.3+ service built with Java 21 that processes export requests from SQS, extracts ZIP files, and uploads content to S3. Designed for deployment on AWS Fargate with virtual threads for high-throughput I/O operations.

## Features

- **Virtual Threads (Java 21)** - Lightweight concurrency for efficient I/O-bound operations
- **SQS Message Processing** - Consumes export requests with automatic visibility timeout extension
- **Streaming ZIP Extraction** - Memory-efficient processing of large ZIP files using Apache Commons Compress
- **S3 Multipart Uploads** - Automatic multipart uploads for large files via S3 Transfer Manager
- **DynamoDB Job Tracking** - Idempotent processing with distributed locking
- **Resilience Patterns** - Retry and circuit breaker via Resilience4j
- **Observability** - Prometheus metrics, structured JSON logging, health probes
- **Graceful Shutdown** - Proper message handling during container termination

## Architecture

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────┐
│   SQS       │────▶│  Export          │────▶│   S3        │
│   Queue     │     │  Processor       │     │   Bucket    │
└─────────────┘     └──────────────────┘     └─────────────┘
                           │
                           ▼
                    ┌──────────────┐
                    │  DynamoDB    │
                    │  (Job Track) │
                    └──────────────┘
```

## Prerequisites

| Software | Version | Verification Command |
|----------|---------|---------------------|
| Java JDK | 21+     | `java -version`     |
| Maven    | 3.9+    | `mvn -version`      |
| Docker   | 24+     | `docker --version`  |
| AWS CLI  | 2.x     | `aws --version`     |

### Install Java 21 (if needed)

```bash
# Using SDKMAN (recommended)
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.2-tem
```

## Project Structure

```
export-processor/
├── pom.xml                          # Maven configuration
├── Dockerfile                       # Multi-stage Docker build
├── docker-compose.yml               # Local development stack
├── localstack-init/
│   └── init-aws.sh                  # AWS resource initialization
├── mock-api/
│   └── export-api.json              # Mock Export API configuration
├── src/
│   ├── main/
│   │   ├── java/com/yourcompany/exportprocessor/
│   │   │   ├── ExportProcessorApplication.java
│   │   │   ├── config/
│   │   │   │   ├── AwsConfig.java
│   │   │   │   ├── DynamoDbConfig.java
│   │   │   │   ├── S3Config.java
│   │   │   │   ├── ResilienceConfig.java
│   │   │   │   └── JacksonConfig.java
│   │   │   ├── model/
│   │   │   │   ├── ExportRequest.java
│   │   │   │   ├── ExportRecord.java
│   │   │   │   └── JobTracking.java
│   │   │   ├── repository/
│   │   │   │   └── JobTrackingRepository.java
│   │   │   ├── service/
│   │   │   │   ├── ExportService.java
│   │   │   │   ├── ZipExtractionService.java
│   │   │   │   ├── S3UploadService.java
│   │   │   │   └── HeartbeatService.java
│   │   │   ├── client/
│   │   │   │   └── ExportApiClient.java
│   │   │   └── listener/
│   │   │       └── ExportMessageListener.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       ├── application-prod.yml
│   │       └── logback-spring.xml
│   └── test/
│       ├── java/com/yourcompany/exportprocessor/
│       │   ├── ExportProcessorApplicationTests.java
│       │   ├── integration/
│       │   │   └── ExportProcessingIntegrationTest.java
│       │   └── service/
│       │       └── ExportServiceTest.java
│       └── resources/
│           └── application-test.yml
```

## Quick Start

### Option 1: Docker Compose (Recommended for Development)

```bash
# Start all services (LocalStack + Application + Mock API)
docker-compose up --build

# In another terminal, send a test message
awslocal sqs send-message \
    --queue-url http://localhost:4566/000000000000/export-requests \
    --message-body '{
        "jobId": "job-001",
        "exportId": "export-001",
        "callbackUrl": "https://webhook.example.com/callback",
        "metadata": {
            "customerId": "cust-123",
            "requestedBy": "user@example.com",
            "exportType": "FULL"
        }
    }'
```

### Option 2: Maven + Docker Compose (Recommended for Local Development)

This option uses Docker Compose for LocalStack and Mock API, while running the Spring Boot application directly with Maven for faster development iterations.

```bash
# 1. Start LocalStack and Mock API
docker-compose up -d localstack mock-api

# 2. Wait for LocalStack to initialize (check logs)
docker logs -f fargate-java-localstack-1
# Wait until you see "Ready." and "LocalStack initialization complete!"
# Press Ctrl+C to exit logs

# 3. Start the Spring Boot application
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 4. In another terminal, send a test message
awslocal sqs send-message \
  --queue-url http://localhost:4566/000000000000/export-requests \
  --message-body '{"jobId":"job-success-test","exportId":"export-001","callbackUrl":"https://webhook.example.com/callback","metadata":{"customerId":"cust-123"}}'

# 5. Verify successful processing
# Check S3 for uploaded files
awslocal s3 ls s3://export-outputs/ --recursive

# Check DynamoDB for completed job
awslocal dynamodb scan --table-name job-tracking --filter-expression "jobId = :jid" \
  --expression-attribute-values '{":jid":{"S":"job-success-test"}}'
```

**Troubleshooting Tips:**

- If the application fails to connect, ensure LocalStack is fully initialized
- Run `awslocal s3 ls` to verify the S3 bucket exists
- Run `awslocal sqs list-queues` to verify queues are created
- To restart the application: `pkill -f 'ExportProcessorApplication' && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`

### Option 3: Standalone LocalStack (Advanced)

```bash
# Start LocalStack separately
docker run -d --name localstack -p 4566:4566 localstack/localstack:3.0

# Initialize AWS resources manually
./localstack-init/init-aws.sh

# Start Mock API (if needed for testing)
docker run -d -p 8081:8081 -v ./mock-api:/data mockoon/cli:latest \
  --data /data/export-api.json --port 8081

# Run the application
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## Build Commands

```bash
# Clean build
./mvnw clean package

# Skip tests
./mvnw clean package -DskipTests

# Run unit tests
./mvnw test

# Run integration tests
./mvnw verify

# Build Docker image
docker build -t export-processor:latest .
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `AWS_REGION` | AWS region | `us-east-1` |
| `SQS_EXPORT_QUEUE` | SQS queue name | `export-requests` |
| `S3_OUTPUT_BUCKET` | S3 output bucket | `export-outputs` |
| `DYNAMODB_JOB_TABLE` | DynamoDB table name | `job-tracking` |
| `EXPORT_API_URL` | Export API base URL | `https://api.example.com` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | (none) |

### Key Configuration Properties

```yaml
# Virtual Threads (Critical for performance)
spring.threads.virtual.enabled: true

# SQS Listener (Single message processing)
spring.cloud.aws.sqs.listener:
  max-concurrent-messages: 1
  max-messages-per-poll: 1
  poll-timeout: 20s

# Processing Settings
app.processing:
  heartbeat-interval-seconds: 120
  checkpoint-interval-seconds: 300
  max-concurrent-uploads: 5
  multipart-threshold-mb: 100

# Resilience4j Retry
resilience4j.retry.instances.export-api:
  max-attempts: 5
  wait-duration: 1s
  exponential-backoff-multiplier: 2

# Resilience4j Circuit Breaker
resilience4j.circuitbreaker.instances.export-api:
  sliding-window-size: 10
  failure-rate-threshold: 50
  wait-duration-in-open-state: 60s
```

## Message Format

### SQS Message Body (ExportRequest)

```json
{
    "jobId": "job-001",
    "exportId": "export-001",
    "callbackUrl": "https://webhook.example.com/callback",
    "metadata": {
        "customerId": "cust-123",
        "requestedBy": "user@example.com",
        "exportType": "FULL"
    }
}
```

## Monitoring

### Health Endpoints

```bash
# Liveness probe (Kubernetes)
curl http://localhost:8080/actuator/health/liveness

# Readiness probe (Kubernetes)
curl http://localhost:8080/actuator/health/readiness

# Full health details
curl http://localhost:8080/actuator/health

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# Application info
curl http://localhost:8080/actuator/info
```

### Exposed Actuator Endpoints

- `/actuator/health` - Health status with details
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus-compatible metrics
- `/actuator/info` - Application information

## Processing Flow

1. **Message Reception** - SQS listener receives export request
2. **Idempotency Check** - DynamoDB conditional write to claim job
3. **Heartbeat Start** - Background thread extends SQS visibility timeout
4. **Export Download** - Fetch ZIP from Export API (with retry/circuit breaker)
5. **ZIP Extraction** - Stream ZIP entries using Apache Commons Compress
6. **S3 Upload** - Upload each file to S3 (multipart for large files)
7. **Checkpoint** - Periodic progress saves to DynamoDB
8. **Completion** - Mark job complete, acknowledge SQS message

## Key Dependencies

| Library | Purpose | Version |
|---------|---------|---------|
| Spring Boot | Framework | 3.3.0 |
| Spring Cloud AWS | SQS, S3 integration | 3.2.0 |
| AWS SDK v2 | DynamoDB, Transfer Manager | 2.28.0 |
| Apache Commons Compress | ZIP extraction | 1.26.2 |
| Resilience4j | Retry, Circuit Breaker | 2.2.0 |
| Micrometer | Metrics | (managed) |
| Logstash Logback Encoder | JSON logging | 7.4 |
| Testcontainers | Integration testing | 1.20.0 |

## Docker

### Build Image

```bash
docker build -t export-processor:latest .
```

### Run Container

```bash
docker run -d \
  -p 8080:8080 \
  -e AWS_REGION=us-east-1 \
  -e AWS_ACCESS_KEY_ID=your-key \
  -e AWS_SECRET_ACCESS_KEY=your-secret \
  -e SQS_EXPORT_QUEUE=export-requests \
  -e S3_OUTPUT_BUCKET=export-outputs \
  -e DYNAMODB_JOB_TABLE=job-tracking \
  -e EXPORT_API_URL=https://api.example.com \
  export-processor:latest
```

### JVM Settings (Container Optimized)

The Dockerfile configures optimal JVM settings for containers:

```
-XX:+UseZGC                    # Z Garbage Collector (low latency)
-XX:MaxRAMPercentage=75.0      # Use 75% of container memory
-XX:+UseStringDeduplication    # Reduce memory for duplicate strings
```

## AWS Infrastructure

### Required AWS Resources

1. **SQS Queue** - `export-requests` (with DLQ)
2. **S3 Bucket** - `export-outputs`
3. **DynamoDB Table** - `job-tracking`
   - Partition Key: `pk` (String)
   - Sort Key: `sk` (String)
   - GSI: `status-index` on `status`
   - TTL: Enabled on `ttl` attribute

### IAM Permissions Required

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "sqs:ReceiveMessage",
                "sqs:DeleteMessage",
                "sqs:ChangeMessageVisibility",
                "sqs:GetQueueUrl"
            ],
            "Resource": "arn:aws:sqs:*:*:export-requests"
        },
        {
            "Effect": "Allow",
            "Action": [
                "s3:PutObject",
                "s3:PutObjectAcl"
            ],
            "Resource": "arn:aws:s3:::export-outputs/*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "dynamodb:GetItem",
                "dynamodb:PutItem",
                "dynamodb:UpdateItem",
                "dynamodb:Query"
            ],
            "Resource": [
                "arn:aws:dynamodb:*:*:table/job-tracking",
                "arn:aws:dynamodb:*:*:table/job-tracking/index/*"
            ]
        }
    ]
}
```

## Testing

### Unit Tests

```bash
./mvnw test
```

### Integration Tests (with Testcontainers)

```bash
./mvnw verify
```

Integration tests use Testcontainers to spin up LocalStack automatically.

### Manual Testing with LocalStack

```bash
# List SQS queues
awslocal sqs list-queues

# Check S3 bucket contents
awslocal s3 ls s3://export-outputs/

# Scan DynamoDB table
awslocal dynamodb scan --table-name job-tracking
```

## Troubleshooting

### Common Issues

**1. Queue not found**
```
Ensure LocalStack is running and init-aws.sh has been executed.
```

**2. Connection refused to LocalStack**
```
Check that SPRING_CLOUD_AWS_ENDPOINT is set correctly for local profile.
```

**3. DynamoDB conditional check failed**
```
This is expected behavior when a job is already claimed by another worker.
```

**4. Memory issues with large ZIPs**
```
The service uses streaming - ensure max-concurrent-uploads is appropriate
for your container memory limits.
```

### Logging

Development logging uses console output. Production uses JSON format for log aggregation:

```json
{
  "timestamp": "2024-01-15T10:30:00.000Z",
  "level": "INFO",
  "logger": "c.y.e.listener.ExportMessageListener",
  "message": "Export processing completed successfully",
  "messageId": "abc-123",
  "jobId": "job-001"
}
```

## License

This project is provided as a reference implementation.
