#!/bin/bash

echo "Initializing LocalStack resources..."

# Create SQS Queue
awslocal sqs create-queue --queue-name export-requests
awslocal sqs create-queue --queue-name export-requests-dlq

# Set DLQ policy
awslocal sqs set-queue-attributes \
    --queue-url http://localhost:4566/000000000000/export-requests \
    --attributes '{
        "RedrivePolicy": "{\"deadLetterTargetArn\":\"arn:aws:sqs:us-east-1:000000000000:export-requests-dlq\",\"maxReceiveCount\":\"3\"}"
    }'

# Create S3 Bucket
awslocal s3 mb s3://export-outputs

# Create DynamoDB Table
awslocal dynamodb create-table \
    --table-name job-tracking \
    --attribute-definitions \
        AttributeName=pk,AttributeType=S \
        AttributeName=sk,AttributeType=S \
        AttributeName=status,AttributeType=S \
    --key-schema \
        AttributeName=pk,KeyType=HASH \
        AttributeName=sk,KeyType=RANGE \
    --global-secondary-indexes '[
        {
            "IndexName": "status-index",
            "KeySchema": [{"AttributeName":"status","KeyType":"HASH"}],
            "Projection": {"ProjectionType":"ALL"},
            "ProvisionedThroughput": {"ReadCapacityUnits":5,"WriteCapacityUnits":5}
        }
    ]' \
    --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5

# Enable TTL on job-tracking table
awslocal dynamodb update-time-to-live \
    --table-name job-tracking \
    --time-to-live-specification Enabled=true,AttributeName=ttl

echo "LocalStack initialization complete!"
