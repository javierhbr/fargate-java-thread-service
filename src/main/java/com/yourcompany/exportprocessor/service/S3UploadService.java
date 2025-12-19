package com.yourcompany.exportprocessor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3UploadService {

    private final S3AsyncClient s3AsyncClient;
    private final S3TransferManager s3TransferManager;

    @Value("${app.processing.multipart-threshold-mb:100}")
    private long multipartThresholdMb;

    // Virtual thread executor for I/O operations
    private final ExecutorService ioExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Uploads a stream to S3.
     * Uses multipart upload for large files automatically.
     */
    public void uploadStream(String bucket, String key, InputStream inputStream, long contentLength) {

        if (contentLength > multipartThresholdMb * 1024 * 1024) {
            // Use Transfer Manager for large files
            uploadWithTransferManager(bucket, key, inputStream, contentLength);
        } else {
            // Direct upload for small files
            uploadDirect(bucket, key, inputStream, contentLength);
        }
    }

    private void uploadDirect(String bucket, String key, InputStream inputStream, long contentLength) {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentLength(contentLength)
                    .build();

            s3AsyncClient.putObject(putRequest, AsyncRequestBody.fromInputStream(
                    inputStream, contentLength, ioExecutor
            )).join();

            log.debug("Uploaded file: s3://{}/{} ({}bytes)", bucket, key, contentLength);

        } catch (Exception e) {
            log.error("Failed to upload: s3://{}/{}", bucket, key, e);
            throw new RuntimeException("S3 upload failed", e);
        }
    }

    private void uploadWithTransferManager(String bucket, String key, InputStream inputStream, long contentLength) {
        try {
            Upload upload = s3TransferManager.upload(UploadRequest.builder()
                    .putObjectRequest(req -> req.bucket(bucket).key(key))
                    .requestBody(AsyncRequestBody.fromInputStream(
                            inputStream, contentLength, ioExecutor))
                    .build());

            upload.completionFuture().join();

            log.debug("Uploaded large file: s3://{}/{} ({}bytes)", bucket, key, contentLength);

        } catch (Exception e) {
            log.error("Failed to upload large file: s3://{}/{}", bucket, key, e);
            throw new RuntimeException("S3 multipart upload failed", e);
        }
    }
}
