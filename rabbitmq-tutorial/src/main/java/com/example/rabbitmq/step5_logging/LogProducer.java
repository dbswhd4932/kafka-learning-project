package com.example.rabbitmq.step5_logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Step 5: 로그 메시지 Producer
 *
 * 로그 메시지를 Topic Exchange를 통해 전송합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.logging.exchange-name}")
    private String exchangeName;

    /**
     * 로그 메시지 전송
     */
    public void sendLog(LogMessage logMessage) {
        String routingKey = logMessage.getRoutingKey();

        log.info("=== 로그 메시지 전송 ===");
        log.info("Exchange: {}", exchangeName);
        log.info("Routing Key: {}", routingKey);
        log.info("Level: {}", logMessage.getLevel());
        log.info("Service: {}", logMessage.getServiceName());
        log.info("Message: {}", logMessage.getMessage());

        rabbitTemplate.convertAndSend(exchangeName, routingKey, logMessage);

        log.info("로그 메시지 전송 완료");
    }

    /**
     * 간편 로그 전송 메서드들
     */
    public void error(String serviceName, String message) {
        sendLog(new LogMessage(LogMessage.LogLevel.ERROR, serviceName, message));
    }

    public void warn(String serviceName, String message) {
        sendLog(new LogMessage(LogMessage.LogLevel.WARN, serviceName, message));
    }

    public void info(String serviceName, String message) {
        sendLog(new LogMessage(LogMessage.LogLevel.INFO, serviceName, message));
    }

    public void debug(String serviceName, String message) {
        sendLog(new LogMessage(LogMessage.LogLevel.DEBUG, serviceName, message));
    }
}
