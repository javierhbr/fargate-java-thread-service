package com.yourcompany.exportprocessor.listener;

import com.yourcompany.exportprocessor.model.ExportRequest;
import com.yourcompany.exportprocessor.repository.JobTrackingRepository;
import com.yourcompany.exportprocessor.service.ExportService;
import com.yourcompany.exportprocessor.service.HeartbeatService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.annotation.SqsListenerAcknowledgementMode;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExportMessageListener {

    private final ExportService exportService;
    private final JobTrackingRepository jobTrackingRepository;
    private final HeartbeatService heartbeatService;

    @SqsListener(value = "${app.sqs.export-queue}", acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL)
    public void processExport(
            @Payload ExportRequest request,
            @Header(MessageHeaders.ID) String messageId,
            @Header(SqsHeaders.SQS_RECEIPT_HANDLE_HEADER) String receiptHandle,
            Acknowledgement acknowledgement) {

        MDC.put("messageId", messageId);
        MDC.put("jobId", request.getJobId());

        log.info("Received export request: jobId={}, exportId={}",
                request.getJobId(), request.getExportId());

        ScheduledFuture<?> heartbeat = null;

        try {
            // 1. Idempotency check - try to claim the job
            if (!jobTrackingRepository.claimJob(messageId, request.getJobId())) {
                log.info("Job already claimed by another worker, acknowledging message");
                acknowledgement.acknowledge();
                return;
            }

            // 2. Start heartbeat to extend visibility timeout
            heartbeat = heartbeatService.startHeartbeat(receiptHandle);

            // 3. Process the export
            exportService.processExport(request, messageId);

            // 4. Mark job as completed
            jobTrackingRepository.markCompleted(messageId);

            // 5. Acknowledge the message
            acknowledgement.acknowledge();

            log.info("Export processing completed successfully");

        } catch (Exception e) {
            log.error("Export processing failed", e);
            jobTrackingRepository.markFailed(messageId, e.getMessage());
            // Don't acknowledge - let SQS retry or send to DLQ
            throw new RuntimeException("Export processing failed", e);

        } finally {
            // Stop heartbeat
            if (heartbeat != null) {
                heartbeat.cancel(false);
            }
            MDC.clear();
        }
    }
}
