package com.yourcompany.exportprocessor.service;

import com.yourcompany.exportprocessor.client.ExportApiClient;
import com.yourcompany.exportprocessor.model.ExportRequest;
import com.yourcompany.exportprocessor.repository.JobTrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private ExportApiClient exportApiClient;

    @Mock
    private ZipExtractionService zipExtractionService;

    @Mock
    private S3UploadService s3UploadService;

    @Mock
    private JobTrackingRepository jobTrackingRepository;

    private ExportService exportService;

    @BeforeEach
    void setUp() {
        exportService = new ExportService(
                exportApiClient,
                zipExtractionService,
                s3UploadService,
                jobTrackingRepository
        );
    }

    @Test
    void processExport_shouldDownloadAndExtractZip() {
        // Given
        ExportRequest request = ExportRequest.builder()
                .jobId("job-123")
                .exportId("export-456")
                .metadata(ExportRequest.ExportMetadata.builder()
                        .customerId("cust-789")
                        .build())
                .build();
        String messageId = "msg-001";

        InputStream mockStream = new ByteArrayInputStream(new byte[0]);
        when(exportApiClient.downloadExport("export-456")).thenReturn(mockStream);
        when(zipExtractionService.extractAndUpload(any(), anyString(), anyString(), any()))
                .thenReturn(10);

        // When
        exportService.processExport(request, messageId);

        // Then
        verify(exportApiClient).downloadExport("export-456");
        verify(zipExtractionService).extractAndUpload(
                eq(mockStream),
                anyString(),
                contains("exports/cust-789/job-123/"),
                any()
        );
    }
}
