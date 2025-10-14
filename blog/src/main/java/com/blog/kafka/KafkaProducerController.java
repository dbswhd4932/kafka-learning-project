package com.blog.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 카프카 메시지를 발행(produce)하는 역할을 담당하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/kafka")
public class KafkaProducerController {

    private static final String TOPIC_NAME = "my-topic"; // 메시지를 보낼 토픽 이름

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaProducerController(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * HTTP POST 요청을 받아 카프카 토픽으로 메시지를 보냅니다.
     *
     * @param message 요청 본문에 포함된 메시지 문자열
     * @return 성공 메시지
     */
    @PostMapping("/publish")
    public String publishMessage(@RequestBody String message) {
        // KafkaTemplate을 사용하여 지정된 토픽으로 메시지를 보냅니다.
        kafkaTemplate.send(TOPIC_NAME, message);
        return "Message published successfully";
    }
}
