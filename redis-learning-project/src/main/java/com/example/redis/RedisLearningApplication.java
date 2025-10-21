package com.example.redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class RedisLearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedisLearningApplication.class, args);
    }

}
