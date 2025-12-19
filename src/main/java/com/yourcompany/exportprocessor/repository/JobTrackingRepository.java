package com.yourcompany.exportprocessor.repository;

import com.yourcompany.exportprocessor.model.JobTracking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class JobTrackingRepository {

    private static final Duration LOCK_DURATION = Duration.ofMinutes(30);
    private static final Duration TTL_DURATION = Duration.ofHours(48);

    private final DynamoDbTable<JobTracking> jobTrackingTable;

    /**
     * Attempts to claim a job using conditional write.
     * Returns true if this worker successfully claimed the job.
     */
    public boolean claimJob(String messageId, String jobId) {
        String pk = JobTracking.createPk(messageId);
        Instant now = Instant.now();
        Instant expiry = now.plus(LOCK_DURATION);
        Instant ttl = now.plus(TTL_DURATION);

        JobTracking job = JobTracking.builder()
                .pk(pk)
                .sk("JOB")
                .status(JobTracking.Status.IN_PROGRESS)
                .jobId(jobId)
                .inProgressExpiry(expiry)
                .workerId(UUID.randomUUID().toString())
                .createdAt(now)
                .updatedAt(now)
                .ttl(ttl.getEpochSecond())
                .build();

        // Condition: Item doesn't exist OR lock has expired
        Expression condition = Expression.builder()
                .expression("attribute_not_exists(pk) OR inProgressExpiry < :now")
                .putExpressionValue(":now",
                    software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder()
                        .s(now.toString())
                        .build())
                .build();

        try {
            jobTrackingTable.putItem(PutItemEnhancedRequest.builder(JobTracking.class)
                    .item(job)
                    .conditionExpression(condition)
                    .build());

            log.debug("Successfully claimed job: messageId={}", messageId);
            return true;

        } catch (ConditionalCheckFailedException e) {
            log.debug("Job already claimed: messageId={}", messageId);
            return false;
        }
    }

    /**
     * Updates job status to COMPLETED.
     */
    public void markCompleted(String messageId) {
        updateStatus(messageId, JobTracking.Status.COMPLETED, null);
    }

    /**
     * Updates job status to FAILED with error message.
     */
    public void markFailed(String messageId, String errorMessage) {
        updateStatus(messageId, JobTracking.Status.FAILED, errorMessage);
    }

    /**
     * Updates checkpoint data for long-running jobs.
     */
    public void updateCheckpoint(String messageId, String checkpointData, int recordsProcessed) {
        String pk = JobTracking.createPk(messageId);

        JobTracking existing = jobTrackingTable.getItem(r -> r.key(k -> k.partitionValue(pk).sortValue("JOB")));

        if (existing != null) {
            existing.setCheckpointData(checkpointData);
            existing.setRecordsProcessed(recordsProcessed);
            existing.setUpdatedAt(Instant.now());
            existing.setInProgressExpiry(Instant.now().plus(LOCK_DURATION));
            jobTrackingTable.putItem(existing);
        }
    }

    private void updateStatus(String messageId, JobTracking.Status status, String errorMessage) {
        String pk = JobTracking.createPk(messageId);

        JobTracking existing = jobTrackingTable.getItem(r -> r.key(k -> k.partitionValue(pk).sortValue("JOB")));

        if (existing != null) {
            existing.setStatus(status);
            existing.setUpdatedAt(Instant.now());
            if (errorMessage != null) {
                existing.setErrorMessage(errorMessage);
            }
            jobTrackingTable.putItem(existing);
        }
    }
}
