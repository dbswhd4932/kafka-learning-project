package com.example.async.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class EmailService {

    /**
     * 비동기로 이메일 전송 (반환값 없음)
     */
    @Async("emailExecutor")
    public void sendEmail(String to, String subject, String body) {
        log.info("[{}] Sending email to: {} - Subject: {}",
                Thread.currentThread().getName(), to, subject);

        try {
            // 이메일 전송 시뮬레이션 (3초 소요)
            Thread.sleep(3000);
            log.info("[{}] Email sent successfully to: {}",
                    Thread.currentThread().getName(), to);
        } catch (InterruptedException e) {
            log.error("Email sending interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 비동기로 이메일 전송 (CompletableFuture 반환)
     */
    @Async("emailExecutor")
    public CompletableFuture<Boolean> sendEmailWithResult(String to, String subject, String body) {
        log.info("[{}] Sending email with result to: {} - Subject: {}",
                Thread.currentThread().getName(), to, subject);

        try {
            // 이메일 전송 시뮬레이션 (3초 소요)
            Thread.sleep(3000);
            log.info("[{}] Email sent successfully to: {}",
                    Thread.currentThread().getName(), to);
            return CompletableFuture.completedFuture(true);
        } catch (InterruptedException e) {
            log.error("Email sending interrupted", e);
            Thread.currentThread().interrupt();
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * 대량 이메일 전송 (비동기)
     */
    @Async("emailExecutor")
    public CompletableFuture<Integer> sendBulkEmails(String[] recipients, String subject, String body) {
        log.info("[{}] Sending bulk emails to {} recipients",
                Thread.currentThread().getName(), recipients.length);

        int successCount = 0;
        for (String recipient : recipients) {
            try {
                Thread.sleep(1000); // 각 이메일당 1초
                log.info("[{}] Email sent to: {}", Thread.currentThread().getName(), recipient);
                successCount++;
            } catch (InterruptedException e) {
                log.error("Bulk email sending interrupted", e);
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.info("[{}] Bulk email sending completed. Success: {}/{}",
                Thread.currentThread().getName(), successCount, recipients.length);
        return CompletableFuture.completedFuture(successCount);
    }
}
