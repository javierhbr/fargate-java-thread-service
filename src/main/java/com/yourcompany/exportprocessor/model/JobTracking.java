package com.yourcompany.exportprocessor.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;

@DynamoDbBean
public class JobTracking {

    public enum Status {
        PENDING, IN_PROGRESS, COMPLETED, FAILED
    }

    private String pk;           // msg#{messageId}
    private String sk;           // JOB
    private Status status;
    private Instant inProgressExpiry;
    private String workerId;
    private String jobId;
    private String errorMessage;
    private Integer recordsProcessed;
    private String checkpointData;
    private Instant createdAt;
    private Instant updatedAt;
    private Long ttl;            // Unix timestamp for DynamoDB TTL

    public JobTracking() {
    }

    public JobTracking(String pk, String sk, Status status, Instant inProgressExpiry, String workerId,
                       String jobId, String errorMessage, Integer recordsProcessed, String checkpointData,
                       Instant createdAt, Instant updatedAt, Long ttl) {
        this.pk = pk;
        this.sk = sk;
        this.status = status;
        this.inProgressExpiry = inProgressExpiry;
        this.workerId = workerId;
        this.jobId = jobId;
        this.errorMessage = errorMessage;
        this.recordsProcessed = recordsProcessed;
        this.checkpointData = checkpointData;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.ttl = ttl;
    }

    @DynamoDbPartitionKey
    public String getPk() {
        return pk;
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    @DynamoDbSortKey
    public String getSk() {
        return sk;
    }

    public void setSk(String sk) {
        this.sk = sk;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "status-index")
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Instant getInProgressExpiry() {
        return inProgressExpiry;
    }

    public void setInProgressExpiry(Instant inProgressExpiry) {
        this.inProgressExpiry = inProgressExpiry;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getRecordsProcessed() {
        return recordsProcessed;
    }

    public void setRecordsProcessed(Integer recordsProcessed) {
        this.recordsProcessed = recordsProcessed;
    }

    public String getCheckpointData() {
        return checkpointData;
    }

    public void setCheckpointData(String checkpointData) {
        this.checkpointData = checkpointData;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }

    // Helper method to create PK
    public static String createPk(String messageId) {
        return "msg#" + messageId;
    }

    public static JobTrackingBuilder builder() {
        return new JobTrackingBuilder();
    }

    public static class JobTrackingBuilder {
        private String pk;
        private String sk;
        private Status status;
        private Instant inProgressExpiry;
        private String workerId;
        private String jobId;
        private String errorMessage;
        private Integer recordsProcessed;
        private String checkpointData;
        private Instant createdAt;
        private Instant updatedAt;
        private Long ttl;

        public JobTrackingBuilder pk(String pk) {
            this.pk = pk;
            return this;
        }

        public JobTrackingBuilder sk(String sk) {
            this.sk = sk;
            return this;
        }

        public JobTrackingBuilder status(Status status) {
            this.status = status;
            return this;
        }

        public JobTrackingBuilder inProgressExpiry(Instant inProgressExpiry) {
            this.inProgressExpiry = inProgressExpiry;
            return this;
        }

        public JobTrackingBuilder workerId(String workerId) {
            this.workerId = workerId;
            return this;
        }

        public JobTrackingBuilder jobId(String jobId) {
            this.jobId = jobId;
            return this;
        }

        public JobTrackingBuilder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public JobTrackingBuilder recordsProcessed(Integer recordsProcessed) {
            this.recordsProcessed = recordsProcessed;
            return this;
        }

        public JobTrackingBuilder checkpointData(String checkpointData) {
            this.checkpointData = checkpointData;
            return this;
        }

        public JobTrackingBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public JobTrackingBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public JobTrackingBuilder ttl(Long ttl) {
            this.ttl = ttl;
            return this;
        }

        public JobTracking build() {
            return new JobTracking(pk, sk, status, inProgressExpiry, workerId, jobId,
                    errorMessage, recordsProcessed, checkpointData, createdAt, updatedAt, ttl);
        }
    }
}
