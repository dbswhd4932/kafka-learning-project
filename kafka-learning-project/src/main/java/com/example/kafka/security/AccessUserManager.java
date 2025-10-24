package com.example.kafka.security;

import com.example.kafka.enums.UserType;

/**
 * 접근 사용자 관리자
 * - ThreadLocal로 현재 요청의 사용자 정보 관리
 * - 실무에서는 Spring Security의 SecurityContext 사용
 */
public class AccessUserManager {

    private static final ThreadLocal<AccessUser> CURRENT_USER = new ThreadLocal<>();

    /**
     * 현재 사용자 설정
     */
    public static void setAccessUser(AccessUser user) {
        CURRENT_USER.set(user);
    }

    /**
     * 현재 사용자 조회
     * - 없으면 SYSTEM 사용자 반환
     */
    public static AccessUser getAccessUser() {
        AccessUser user = CURRENT_USER.get();
        if (user == null) {
            // 기본값: SYSTEM 사용자
            return AccessUser.system();
        }
        return user;
    }

    /**
     * 현재 사용자 제거
     */
    public static void clear() {
        CURRENT_USER.remove();
    }

    /**
     * 시스템 사용자로 설정
     */
    public static void setSystemUser() {
        setAccessUser(AccessUser.system());
    }
}
