package com.example.redis.service;

import com.example.redis.domain.User;
import com.example.redis.dto.UserResponse;
import com.example.redis.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 사용자 서비스 (Redis 캐싱 적용)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    @Cacheable(value = "users", key = "'all'")
    public List<UserResponse> getAllUsers() {
        log.info("DB에서 모든 사용자 조회 (캐시 미스)");
        simulateSlowQuery();

        List<User> users = userRepository.findAll();
        return users.stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "user", key = "#id")
    public UserResponse getUserById(Long id) {
        log.info("DB에서 사용자 조회 - ID: {} (캐시 미스)", id);
        simulateSlowQuery();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. ID: " + id));
        return UserResponse.from(user);
    }

    @Cacheable(value = "user", key = "'username:' + #username")
    public UserResponse getUserByUsername(String username) {
        log.info("DB에서 사용자 조회 - Username: {} (캐시 미스)", username);
        simulateSlowQuery();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. Username: " + username));
        return UserResponse.from(user);
    }

    private void simulateSlowQuery() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
