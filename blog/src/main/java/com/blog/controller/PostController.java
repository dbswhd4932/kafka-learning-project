package com.blog.controller;

import com.blog.event.UserCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PostController {

    private final ApplicationEventPublisher eventPublisher;

    public PostController(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/users/{username}")
    public String createUser(@PathVariable String username) {
        // In a real application, you would save the user to the database here.
        System.out.println("User creation request for: " + username);

        // Publish the user created event
        eventPublisher.publishEvent(new UserCreatedEvent(this, username));

        return "User " + username + " created successfully.";
    }
}
