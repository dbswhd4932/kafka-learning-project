package com.example.kafka.streams;

import com.example.kafka.message.SalesOrderMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 예제 1: 고액 주문 필터링 (Stateless)
 *
 * 기능:
 * - sales-orders 토픽에서 주문 읽기
 * - 100만원 이상 주문만 필터링
 * - high-value-orders 토픽으로 발행
 *
 * Stateless: 각 메시지를 독립적으로 처리 (이전 상태 불필요)
 */
@Slf4j
@Component  // 주석 처리하면 Spring Bean으로 등록되지 않음
@RequiredArgsConstructor
public class HighValueOrderStream {

    private final ObjectMapper objectMapper;

    /**
     * Kafka Streams Topology 구성
     * @Autowired로 StreamsBuilder를 주입받아 토폴로지 정의
     *
     *   동작 원리
     *   Spring Boot가 @Component 클래스를 스캔할 때:
     *   1. @Autowired 메서드를 찾음
     *   2. 파라미터 타입이 StreamsBuilder면 자동으로 주입
     *   3. 메서드 실행 → Topology 구성 완료
     */
    @Autowired
    public void buildPipeline(StreamsBuilder streamsBuilder) {
        // 1. 소스 토픽에서 KStream 생성
        KStream<String, String> sourceStream = streamsBuilder
                .stream("sales-orders", Consumed.with(Serdes.String(), Serdes.String()));

        // 2. 고액 주문 필터링 및 로깅
        sourceStream
                // JSON → SalesOrderMessage 변환
                .mapValues(this::parseOrderMessage)

                // null 제거 (파싱 실패한 메시지)
                .filter((key, order) -> order != null)

                // 100만원 이상만 필터링
                .filter((key, order) -> {
                    BigDecimal threshold = new BigDecimal("1000000");
                    boolean isHighValue = order.getTotalAmount().compareTo(threshold) >= 0;

                    if (isHighValue) {
                        log.info("💰 고액 주문 감지: {} - {} ({}원)",
                                order.getOrderId(),
                                order.getProductName(),
                                order.getTotalAmount());
                    }

                    return isHighValue;
                })

                // SalesOrderMessage → JSON 변환
                .mapValues(this::toJson)

                // 결과를 high-value-orders 토픽으로 발행
                .to("high-value-orders", Produced.with(Serdes.String(), Serdes.String()));

        log.info("✅ High Value Order Stream 초기화 완료");
    }

    /**
     * JSON 문자열을 SalesOrderMessage로 변환
     */
    private SalesOrderMessage parseOrderMessage(String json) {
        try {
            return objectMapper.readValue(json, SalesOrderMessage.class);
        } catch (Exception e) {
            log.error("Failed to parse order message: {}", json, e);
            return null;
        }
    }

    /**
     * SalesOrderMessage를 JSON 문자열로 변환
     */
    private String toJson(SalesOrderMessage order) {
        try {
            return objectMapper.writeValueAsString(order);
        } catch (Exception e) {
            log.error("Failed to convert order to JSON: {}", order, e);
            return null;
        }
    }
}
