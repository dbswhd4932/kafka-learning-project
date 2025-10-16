package com.example.rabbitmq.step3_routing;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Step 3-3: Fanout Exchange 설정
 *
 * Fanout Exchange는 Routing Key를 무시하고 연결된 모든 Queue로 메시지를 브로드캐스트합니다.
 *
 * 특징:
 * - Routing Key를 무시함
 * - 연결된 모든 Queue에 메시지 복사본 전송
 * - Pub/Sub 패턴 구현
 * - 가장 빠른 Exchange 타입 (라우팅 로직 없음)
 *
 * 사용 사례:
 * - 실시간 알림 (이메일, SMS, 푸시)
 * - 이벤트 브로드캐스팅
 * - 캐시 무효화
 * - 실시간 대시보드 업데이트
 * - 로그 수집 (여러 시스템에 동일한 로그 전달)
 *
 * 동작 방식:
 * Producer → Fanout Exchange → Queue 1
 *                           → Queue 2
 *                           → Queue 3
 *
 * 모든 Queue가 동일한 메시지를 받습니다.
 */
@Configuration
public class FanoutExchangeConfig {

    public static final String FANOUT_EXCHANGE = "fanout.exchange";
    public static final String FANOUT_QUEUE_1 = "fanout.queue.1";
    public static final String FANOUT_QUEUE_2 = "fanout.queue.2";
    public static final String FANOUT_QUEUE_3 = "fanout.queue.3";

    /**
     * Fanout Exchange 생성
     */
    @Bean
    public FanoutExchange routingFanoutExchange() {
        return new FanoutExchange(FANOUT_EXCHANGE);
    }

    /**
     * Fanout Queue 1 생성
     */
    @Bean
    public Queue fanoutQueue1() {
        return new Queue(FANOUT_QUEUE_1, true);
    }

    /**
     * Fanout Queue 2 생성
     */
    @Bean
    public Queue fanoutQueue2() {
        return new Queue(FANOUT_QUEUE_2, true);
    }

    /**
     * Fanout Queue 3 생성
     */
    @Bean
    public Queue fanoutQueue3() {
        return new Queue(FANOUT_QUEUE_3, true);
    }

    /**
     * Fanout Binding 1
     * Routing Key는 무시되지만 Binding은 필요함
     */
    @Bean
    public Binding fanoutBinding1(Queue fanoutQueue1, FanoutExchange routingFanoutExchange) {
        return BindingBuilder.bind(fanoutQueue1).to(routingFanoutExchange);
    }

    /**
     * Fanout Binding 2
     */
    @Bean
    public Binding fanoutBinding2(Queue fanoutQueue2, FanoutExchange routingFanoutExchange) {
        return BindingBuilder.bind(fanoutQueue2).to(routingFanoutExchange);
    }

    /**
     * Fanout Binding 3
     */
    @Bean
    public Binding fanoutBinding3(Queue fanoutQueue3, FanoutExchange routingFanoutExchange) {
        return BindingBuilder.bind(fanoutQueue3).to(routingFanoutExchange);
    }
}
