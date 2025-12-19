package com.yourcompany.exportprocessor.service;

import com.yourcompany.exportprocessor.client.ExportApiClient;
import com.yourcompany.exportprocessor.model.ExportRequest;
import com.yourcompany.exportprocessor.repository.JobTrackingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    private final ExportApiClient exportApiClient;
    private final ZipExtractionService zipExtractionService;
    private final S3UploadService s3UploadService;
    private final JobTrackingRepository jobTrackingRepository;

    @Value("${app.s3.output-bucket}")
    private String outputBucket;

    @Value("${app.processing.checkpoint-interval-seconds:300}")
    private int checkpointIntervalSeconds;

    public ExportService(ExportApiClient exportApiClient,
                         ZipExtractionService zipExtractionService,
                         S3UploadService s3UploadService,
                         JobTrackingRepository jobTrackingRepository) {
        this.exportApiClient = exportApiClient;
        this.zipExtractionService = zipExtractionService;
        this.s3UploadService = s3UploadService;
        this.jobTrackingRepository = jobTrackingRepository;
    }

    public void processExport(ExportRequest request, String messageId) {
        log.info("Starting export processing: exportId={}", request.getExportId());

        // 1. Download export data from Export API
        log.debug("Downloading export data from API");
        InputStream exportStream = exportApiClient.downloadExport(request.getExportId());

        // 2. Process ZIP stream and upload to S3
        log.debug("Processing ZIP stream");
        String outputPrefix = String.format("exports/%s/%s/",
                request.getMetadata().getCustomerId(),
                request.getJobId());

        int recordsProcessed = zipExtractionService.extractAndUpload(
                exportStream,
                outputBucket,
                outputPrefix,
                (checkpoint, count) -> {
                    // Periodic checkpoint callback
                    jobTrackingRepository.updateCheckpoint(messageId, checkpoint, count);
                    log.debug("Checkpoint saved: records={}", count);
                }
        );

        log.info("Export processing completed: recordsProcessed={}", recordsProcessed);
    }
}
