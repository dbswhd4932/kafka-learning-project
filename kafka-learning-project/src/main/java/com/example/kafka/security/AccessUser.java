package com.example.kafka.security;

import com.example.kafka.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 접근 사용자 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessUser {
    private Long userId;
    private String username;
    private UserType userType;

    /**
     * 시스템 사용자 생성
     */
    public static AccessUser system() {
        return AccessUser.builder()
                .userId(0L)
                .username("system")
                .userType(UserType.SYSTEM)
                .build();
    }
}
