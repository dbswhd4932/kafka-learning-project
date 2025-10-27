package com.example.kafka.consumer;

import com.example.kafka.message.SalesOrderMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 고액 주문 Consumer
 *
 * HighValueOrderStream에서 필터링된 100만원 이상 주문을 처리
 *
 * 처리 내용:
 * - VIP 고객 알림
 * - 관리자 알림
 * - 특별 배송 처리
 * - 사기 거래 검증
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HighValueOrderConsumer {

    private final ObjectMapper objectMapper;

    /**
     * 고액 주문 처리
     * - Kafka Streams에서 필터링된 고액 주문만 수신
     */
    @KafkaListener(
            topics = "high-value-orders",
            groupId = "high-value-order-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeHighValueOrders(String message) {
        try {
            SalesOrderMessage order = objectMapper.readValue(message, SalesOrderMessage.class);

            log.info("========================================");
            log.info("💎 고액 주문 수신: {}", order.getOrderId());
            log.info("========================================");

            // 1. 주문 정보 로깅
            logOrderDetails(order);

            // 2. VIP 고객 알림
            notifyVipCustomer(order);

            // 3. 관리자 알림
            notifyAdmin(order);

            // 4. 특별 배송 처리
            processSpecialDelivery(order);

            // 5. 사기 거래 검증
            verifyFraudDetection(order);

            log.info("✅ 고액 주문 처리 완료: {}", order.getOrderId());
            log.info("========================================\n");

        } catch (Exception e) {
            log.error("❌ 고액 주문 처리 실패", e);
        }
    }

    /**
     * 주문 상세 정보 로깅
     */
    private void logOrderDetails(SalesOrderMessage order) {
        log.info("📋 주문 상세:");
        log.info("  - 주문 ID: {}", order.getOrderId());
        log.info("  - 고객 ID: {}", order.getCustomerId());
        log.info("  - 상품명: {}", order.getProductName());
        log.info("  - 수량: {}개", order.getQuantity());
        log.info("  - 단가: {}원", order.getPrice());
        log.info("  - 총 금액: {}원", order.getTotalAmount());
        log.info("  - 주문 시간: {}", order.getOrderDateTime());
    }

    /**
     * VIP 고객 알림 전송
     * 실제로는 SMS, 이메일, 푸시 알림 등 발송
     */
    private void notifyVipCustomer(SalesOrderMessage order) {
        log.info("📱 VIP 고객 알림 전송:");
        log.info("  → 고객 ID: {}", order.getCustomerId());
        log.info("  → 메시지: '{}' 주문이 정상적으로 접수되었습니다.", order.getProductName());
        log.info("  → 특별 혜택: VIP 전용 포장 + 빠른 배송");

        // TODO: 실제 알림 서비스 연동
        // smsService.send(order.getCustomerId(), message);
        // emailService.send(order.getCustomerId(), message);
    }

    /**
     * 관리자 알림 전송
     * 고액 주문 발생 시 관리자에게 즉시 알림
     */
    private void notifyAdmin(SalesOrderMessage order) {
        log.info("🔔 관리자 알림:");
        log.info("  → 고액 주문 발생!");
        log.info("  → 주문 ID: {}", order.getOrderId());
        log.info("  → 금액: {}원", order.getTotalAmount());
        log.info("  → 즉시 확인 필요");

        // TODO: 관리자 대시보드 알림, Slack 알림 등
        // slackService.sendAlert("high-value-order", order);
        // adminDashboard.notify(order);
    }

    /**
     * 특별 배송 처리
     * 고액 주문은 특별 배송 서비스 적용
     */
    private void processSpecialDelivery(SalesOrderMessage order) {
        log.info("🚚 특별 배송 처리:");
        log.info("  → 배송 등급: VIP 프리미엄");
        log.info("  → 예상 배송: 익일 새벽 배송");
        log.info("  → 포장: 고급 선물 포장");
        log.info("  → 배송 추적: 실시간 GPS 추적 제공");

        // TODO: 배송 시스템 연동
        // deliveryService.setVipDelivery(order.getOrderId());
        // packagingService.setGiftWrapping(order.getOrderId());
    }

    /**
     * 사기 거래 검증
     * 고액 주문의 경우 추가 보안 검증
     */
    private void verifyFraudDetection(SalesOrderMessage order) {
        log.info("🔍 사기 거래 검증:");

        // 간단한 검증 로직 (실제로는 ML 모델, 외부 API 등 사용)
        boolean isSuspicious = checkSuspiciousPattern(order);

        if (isSuspicious) {
            log.warn("⚠️  의심 거래 감지!");
            log.warn("  → 주문 ID: {}", order.getOrderId());
            log.warn("  → 추가 검증 필요");
            log.warn("  → 보안팀 확인 요청");

            // TODO: 보안팀 알림, 주문 일시 보류 등
            // securityService.flagForReview(order.getOrderId());
        } else {
            log.info("  → 정상 거래로 판단");
            log.info("  → 검증 통과 ✅");
        }
    }

    /**
     * 의심스러운 패턴 체크
     * 실제로는 복잡한 비즈니스 로직이 들어감
     */
    private boolean checkSuspiciousPattern(SalesOrderMessage order) {
        // 예시: 매우 고액 주문 (500만원 이상)은 추가 검증
        return order.getTotalAmount().doubleValue() >= 5000000;
    }
}
