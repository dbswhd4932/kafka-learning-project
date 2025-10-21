package com.example.redis.controller;

import com.example.redis.dto.UserResponse;
import com.example.redis.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 사용자 컨트롤러
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * 모든 사용자 조회
     * GET /api/users
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        long startTime = System.currentTimeMillis();

        List<UserResponse> users = userService.getAllUsers();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("data", users);
        response.put("count", users.size());
        response.put("duration_ms", duration);

        return ResponseEntity.ok(response);
    }

    /**
     * ID로 사용자 조회
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();

        UserResponse user = userService.getUserById(id);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("data", user);
        response.put("duration_ms", duration);

        return ResponseEntity.ok(response);
    }

    /**
     * Username으로 사용자 조회
     * GET /api/users/username/{username}
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<Map<String, Object>> getUserByUsername(@PathVariable String username) {
        long startTime = System.currentTimeMillis();

        UserResponse user = userService.getUserByUsername(username);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("data", user);
        response.put("duration_ms", duration);

        return ResponseEntity.ok(response);
    }

}
