package com.blog.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * 카프카 메시지를 구독(consume)하는 역할을 담당하는 서비스입니다.
 */
@Service
@Slf4j
public class KafkaConsumerService {

    private static final String TOPIC_NAME = "my-topic"; // 구독할 토픽 이름
    private static final String GROUP_ID = "my-group";   // 컨슈머 그룹 ID

    /**
     * @KafkaListener 어노테이션을 사용하여 지정된 토픽의 메시지를 수신합니다.
     *
     * @param message 수신된 메시지
     */
    @KafkaListener(topics = TOPIC_NAME, groupId = GROUP_ID)
    public void consumeMessage(String message) {
      log.info("전달받은 메시지 : {}", message);
    }
}
