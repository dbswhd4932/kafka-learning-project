package com.example.kafka.streams;

import com.example.kafka.message.SalesOrderMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * 예제 2: 실시간 매출 집계 (Stateful) - 간소화 버전
 *
 * 기능:
 * - sales-orders 토픽에서 주문 읽기
 * - 상품별로 그룹핑
 * - 상품별 주문 건수 카운트
 * - hourly-sales 토픽으로 발행
 *
 * Stateful: 상태를 저장하면서 집계 (누적 계산)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalesAggregationStream {

    private final ObjectMapper objectMapper;

    /**
     * Kafka Streams Topology 구성
     */
    @Autowired
    public void buildPipeline(StreamsBuilder streamsBuilder) {
        // 1. 소스 토픽에서 KStream 생성
        KStream<String, String> sourceStream = streamsBuilder
                .stream("sales-orders", Consumed.with(Serdes.String(), Serdes.String()));

        // 2. 상품별 주문 건수 집계 (간소화 버전)
        sourceStream
                // JSON → SalesOrderMessage 변환
                .mapValues(this::parseOrderMessage)

                // null 제거
                .filter((key, order) -> order != null)

                // 로깅
                .peek((key, order) -> log.info("📊 주문 수신: {} - {} ({}원)",
                        order.getOrderId(), order.getProductName(), order.getTotalAmount()))

                // 상품명을 키로 재그룹핑, 값은 금액 문자열로 변환
                .map((key, order) -> KeyValue.pair(
                        order.getProductName(),
                        order.getTotalAmount().toString()
                ))

                // 상품별 그룹핑
                .groupByKey(Grouped.with(Serdes.String(), Serdes.String()))

                // 1분 단위 시간 윈도우 설정 (Tumbling Window)
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)))

                // 주문 건수 카운트 (간단한 집계)
                .count(Materialized.as("product-sales-count"))

                // Windowed Key → String 변환
                .toStream()
                .map((windowedKey, count) -> {
                    String productName = windowedKey.key();
                    long windowStart = windowedKey.window().start();
                    long windowEnd = windowedKey.window().end();

                    String result = String.format(
                            "{\"product\":\"%s\",\"windowStart\":%d,\"windowEnd\":%d,\"orderCount\":%d}",
                            productName, windowStart, windowEnd, count
                    );

                    log.info("💰 [{}] 1분 주문 집계 완료: {}건 ({}~{})",
                            productName, count, windowStart, windowEnd);

                    return KeyValue.pair(productName, result);
                })

                // 결과를 hourly-sales 토픽으로 발행
                .to("hourly-sales", Produced.with(Serdes.String(), Serdes.String()));

        log.info("✅ Sales Aggregation Stream 초기화 완료");
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
}
