package com.example.async;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AsyncCommunicationApplication {

    public static void main(String[] args) {
        SpringApplication.run(AsyncCommunicationApplication.class, args);
    }
}
