package com.blog.listener;

import com.blog.event.UserCreatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventListener {

    @EventListener
    public void handleUserCreatedEvent(UserCreatedEvent event) {
        System.out.println("New user created: " + event.getUsername() + ". Sending welcome email...");
        // Add logic to send a welcome email or perform other actions
    }
}
