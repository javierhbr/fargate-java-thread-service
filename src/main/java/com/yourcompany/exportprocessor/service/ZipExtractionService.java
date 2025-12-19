package com.yourcompany.exportprocessor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZipExtractionService {

    private final S3UploadService s3UploadService;

    @Value("${app.processing.max-concurrent-uploads:5}")
    private int maxConcurrentUploads;

    @Value("${app.processing.checkpoint-interval-seconds:300}")
    private int checkpointIntervalSeconds;

    /**
     * Extracts ZIP entries and uploads to S3 in a streaming fashion.
     *
     * @param zipStream        Input stream containing ZIP data
     * @param bucket           Target S3 bucket
     * @param prefix           S3 key prefix for uploaded files
     * @param checkpointCallback Callback for progress checkpoints
     * @return Number of records processed
     */
    public int extractAndUpload(
            InputStream zipStream,
            String bucket,
            String prefix,
            BiConsumer<String, Integer> checkpointCallback) {

        AtomicInteger recordCount = new AtomicInteger(0);
        Semaphore uploadSemaphore = new Semaphore(maxConcurrentUploads);
        long lastCheckpoint = System.currentTimeMillis();

        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(
                new BufferedInputStream(zipStream))) {

            ZipArchiveEntry entry;
            while ((entry = zis.getNextZipEntry()) != null) {

                if (entry.isDirectory()) {
                    continue;
                }

                if (!zis.canReadEntryData(entry)) {
                    log.warn("Cannot read entry: {}", entry.getName());
                    continue;
                }

                String s3Key = prefix + sanitizeKey(entry.getName());
                long entrySize = entry.getSize();

                log.debug("Processing entry: name={}, size={}", entry.getName(), entrySize);

                // Acquire semaphore for bounded concurrency
                uploadSemaphore.acquire();

                try {
                    // Upload to S3 (streaming)
                    s3UploadService.uploadStream(bucket, s3Key, zis, entrySize);
                    recordCount.incrementAndGet();

                } finally {
                    uploadSemaphore.release();
                }

                // Periodic checkpoint
                long now = System.currentTimeMillis();
                if (now - lastCheckpoint > checkpointIntervalSeconds * 1000L) {
                    checkpointCallback.accept(entry.getName(), recordCount.get());
                    lastCheckpoint = now;
                }
            }

        } catch (Exception e) {
            log.error("ZIP extraction failed at record {}", recordCount.get(), e);
            throw new RuntimeException("ZIP extraction failed", e);
        }

        return recordCount.get();
    }

    private String sanitizeKey(String entryName) {
        // Remove leading slashes and sanitize path
        return entryName
                .replaceFirst("^/+", "")
                .replace("\\", "/")
                .replaceAll("/+", "/");
    }
}
