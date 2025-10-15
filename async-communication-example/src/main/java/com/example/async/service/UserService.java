package com.example.async.service;

import com.example.async.domain.User;
import com.example.async.dto.UserRequest;
import com.example.async.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * 동기 방식으로 사용자 생성
     */
    @Transactional
    public User createUserSync(UserRequest request) {
        log.info("[{}] Creating user synchronously: {}",
                Thread.currentThread().getName(), request.getEmail());

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .build();

        User savedUser = userRepository.save(user);

        // 동기 방식 - 이메일 전송이 완료될 때까지 대기
        sendWelcomeEmailSync(savedUser);

        log.info("[{}] User created synchronously: {}",
                Thread.currentThread().getName(), savedUser.getId());
        return savedUser;
    }

    /**
     * 비동기 방식으로 사용자 생성
     */
    @Transactional
    public User createUserAsync(UserRequest request) {
        log.info("[{}] Creating user asynchronously: {}",
                Thread.currentThread().getName(), request.getEmail());

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .build();

        User savedUser = userRepository.save(user);

        // 비동기 방식 - 이메일 전송을 기다리지 않고 즉시 반환
        emailService.sendEmail(
                savedUser.getEmail(),
                "Welcome!",
                "Welcome " + savedUser.getName() + " to our service!"
        );

        log.info("[{}] User created asynchronously (email sending in background): {}",
                Thread.currentThread().getName(), savedUser.getId());
        return savedUser;
    }

    /**
     * 사용자 조회 (비동기)
     */
    @Async("taskExecutor")
    public CompletableFuture<User> findUserByIdAsync(Long id) {
        log.info("[{}] Finding user by id asynchronously: {}",
                Thread.currentThread().getName(), id);

        try {
            // 데이터베이스 조회 시뮬레이션
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        User user = userRepository.findById(id).orElse(null);
        log.info("[{}] User found: {}", Thread.currentThread().getName(), user);
        return CompletableFuture.completedFuture(user);
    }

    /**
     * 모든 사용자 조회 (비동기)
     */
    @Async("taskExecutor")
    public CompletableFuture<List<User>> findAllUsersAsync() {
        log.info("[{}] Finding all users asynchronously",
                Thread.currentThread().getName());

        try {
            // 데이터베이스 조회 시뮬레이션
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        List<User> users = userRepository.findAll();
        log.info("[{}] Found {} users", Thread.currentThread().getName(), users.size());
        return CompletableFuture.completedFuture(users);
    }

    /**
     * 동기 방식 환영 이메일 전송
     */
    private void sendWelcomeEmailSync(User user) {
        log.info("[{}] Sending welcome email synchronously to: {}",
                Thread.currentThread().getName(), user.getEmail());
        try {
            Thread.sleep(3000); // 이메일 전송 시뮬레이션
            log.info("[{}] Welcome email sent to: {}",
                    Thread.currentThread().getName(), user.getEmail());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User findUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}
