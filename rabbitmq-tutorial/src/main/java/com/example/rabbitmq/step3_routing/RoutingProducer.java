package com.example.rabbitmq.step3_routing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Step 3: Routing 메시지 Producer
 *
 * Direct, Topic, Fanout Exchange로 메시지를 전송하는 Producer입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoutingProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Direct Exchange로 메시지 전송
     *
     * @param routingKey Routing Key (high, medium, low)
     * @param content 메시지 내용
     */
    public void sendToDirectExchange(String routingKey, String content) {
        log.info("=== Direct Exchange 메시지 전송 ===");
        log.info("Exchange: {}", DirectExchangeConfig.DIRECT_EXCHANGE);
        log.info("Routing Key: {}", routingKey);

        RoutingMessage message = new RoutingMessage("DIRECT", routingKey, content);
        rabbitTemplate.convertAndSend(
                DirectExchangeConfig.DIRECT_EXCHANGE,
                routingKey,
                message
        );

        log.info("메시지 전송 완료: {}", message.getMessageId());
    }

    /**
     * Topic Exchange로 메시지 전송
     *
     * @param routingKey Routing Key (order.created, user.payment.completed 등)
     * @param content 메시지 내용
     */
    public void sendToTopicExchange(String routingKey, String content) {
        log.info("=== Topic Exchange 메시지 전송 ===");
        log.info("Exchange: {}", TopicExchangeConfig.TOPIC_EXCHANGE);
        log.info("Routing Key: {}", routingKey);

        RoutingMessage message = new RoutingMessage("TOPIC", routingKey, content);
        rabbitTemplate.convertAndSend(
                TopicExchangeConfig.TOPIC_EXCHANGE,
                routingKey,
                message
        );

        log.info("메시지 전송 완료: {}", message.getMessageId());
    }

    /**
     * Fanout Exchange로 메시지 전송
     *
     * @param content 메시지 내용
     */
    public void sendToFanoutExchange(String content) {
        log.info("=== Fanout Exchange 메시지 전송 ===");
        log.info("Exchange: {}", FanoutExchangeConfig.FANOUT_EXCHANGE);
        log.info("Routing Key: (ignored)");

        RoutingMessage message = new RoutingMessage("FANOUT", "N/A", content);
        rabbitTemplate.convertAndSend(
                FanoutExchangeConfig.FANOUT_EXCHANGE,
                "", // Routing Key는 무시됨
                message
        );

        log.info("메시지 전송 완료: {}", message.getMessageId());
    }
}
