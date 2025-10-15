package com.example.async.controller;

import com.example.async.domain.User;
import com.example.async.dto.UserRequest;
import com.example.async.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * 동기 방식 사용자 생성
     * GET /api/users/sync - 3초 이상 소요 (이메일 전송 대기)
     */
    @PostMapping("/sync")
    public ResponseEntity<User> createUserSync(@Valid @RequestBody UserRequest request) {
        log.info("Request received: Create user synchronously - {}", request.getEmail());
        long startTime = System.currentTimeMillis();

        User user = userService.createUserSync(request);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Response returned in {}ms", duration);

        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * 비동기 방식 사용자 생성
     * POST /api/users/async - 즉시 반환 (이메일 전송은 백그라운드에서 진행)
     */
    @PostMapping("/async")
    public ResponseEntity<User> createUserAsync(@Valid @RequestBody UserRequest request) {
        log.info("Request received: Create user asynchronously - {}", request.getEmail());
        long startTime = System.currentTimeMillis();

        User user = userService.createUserAsync(request);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Response returned in {}ms (email sending in background)", duration);

        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * 비동기 방식으로 사용자 조회 (CompletableFuture 반환)
     * GET /api/users/{id}/async
     */
    @GetMapping("/{id}/async")
    public CompletableFuture<ResponseEntity<User>> getUserAsync(@PathVariable Long id) {
        log.info("Request received: Get user asynchronously - {}", id);

        return userService.findUserByIdAsync(id)
                .thenApply(user -> {
                    if (user != null) {
                        return ResponseEntity.ok(user);
                    } else {
                        return ResponseEntity.notFound().build();
                    }
                });
    }

    /**
     * 모든 사용자 조회 (CompletableFuture 반환)
     * GET /api/users/async
     */
    @GetMapping("/async")
    public CompletableFuture<ResponseEntity<List<User>>> getAllUsersAsync() {
        log.info("Request received: Get all users asynchronously");

        return userService.findAllUsersAsync()
                .thenApply(ResponseEntity::ok);
    }

    /**
     * 일반 사용자 조회 (동기)
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        log.info("Request received: Get user - {}", id);
        User user = userService.findUserById(id);
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 모든 사용자 조회 (동기)
     * GET /api/users
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        log.info("Request received: Get all users");
        List<User> users = userService.findAllUsers();
        return ResponseEntity.ok(users);
    }
}
