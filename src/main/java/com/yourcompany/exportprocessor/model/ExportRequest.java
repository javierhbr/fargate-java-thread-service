package com.yourcompany.exportprocessor.model;

import jakarta.validation.constraints.NotBlank;

public class ExportRequest {

    @NotBlank
    private String jobId;

    @NotBlank
    private String exportId;

    private String callbackUrl;

    private ExportMetadata metadata;

    public ExportRequest() {
    }

    public ExportRequest(String jobId, String exportId, String callbackUrl, ExportMetadata metadata) {
        this.jobId = jobId;
        this.exportId = exportId;
        this.callbackUrl = callbackUrl;
        this.metadata = metadata;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getExportId() {
        return exportId;
    }

    public void setExportId(String exportId) {
        this.exportId = exportId;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public ExportMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ExportMetadata metadata) {
        this.metadata = metadata;
    }

    public static ExportRequestBuilder builder() {
        return new ExportRequestBuilder();
    }

    public static class ExportRequestBuilder {
        private String jobId;
        private String exportId;
        private String callbackUrl;
        private ExportMetadata metadata;

        public ExportRequestBuilder jobId(String jobId) {
            this.jobId = jobId;
            return this;
        }

        public ExportRequestBuilder exportId(String exportId) {
            this.exportId = exportId;
            return this;
        }

        public ExportRequestBuilder callbackUrl(String callbackUrl) {
            this.callbackUrl = callbackUrl;
            return this;
        }

        public ExportRequestBuilder metadata(ExportMetadata metadata) {
            this.metadata = metadata;
            return this;
        }

        public ExportRequest build() {
            return new ExportRequest(jobId, exportId, callbackUrl, metadata);
        }
    }

    public static class ExportMetadata {
        private String customerId;
        private String requestedBy;
        private String exportType;

        public ExportMetadata() {
        }

        public ExportMetadata(String customerId, String requestedBy, String exportType) {
            this.customerId = customerId;
            this.requestedBy = requestedBy;
            this.exportType = exportType;
        }

        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        public String getRequestedBy() {
            return requestedBy;
        }

        public void setRequestedBy(String requestedBy) {
            this.requestedBy = requestedBy;
        }

        public String getExportType() {
            return exportType;
        }

        public void setExportType(String exportType) {
            this.exportType = exportType;
        }

        public static ExportMetadataBuilder builder() {
            return new ExportMetadataBuilder();
        }

        public static class ExportMetadataBuilder {
            private String customerId;
            private String requestedBy;
            private String exportType;

            public ExportMetadataBuilder customerId(String customerId) {
                this.customerId = customerId;
                return this;
            }

            public ExportMetadataBuilder requestedBy(String requestedBy) {
                this.requestedBy = requestedBy;
                return this;
            }

            public ExportMetadataBuilder exportType(String exportType) {
                this.exportType = exportType;
                return this;
            }

            public ExportMetadata build() {
                return new ExportMetadata(customerId, requestedBy, exportType);
            }
        }
    }
}
