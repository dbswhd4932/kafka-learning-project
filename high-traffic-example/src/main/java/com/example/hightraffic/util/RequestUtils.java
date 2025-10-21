package com.example.hightraffic.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * HTTP 요청 처리 유틸리티
 *
 * 주요 기능:
 * - 클라이언트 실제 IP 주소 추출
 * - 프록시/로드밸런서 환경 지원
 */
@Slf4j
@UtilityClass
public class RequestUtils {

    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR",
            "X-Real-IP"
    };

    /**
     * HttpServletRequest에서 클라이언트의 실제 IP 주소를 추출합니다.
     *
     * 우선순위:
     * 1. X-Forwarded-For 헤더의 첫 번째 IP (프록시 환경)
     * 2. X-Real-IP 헤더 (Nginx 등)
     * 3. request.getRemoteAddr() (직접 연결)
     *
     * @param request HttpServletRequest
     * @return 클라이언트 IP 주소
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "0.0.0.0";
        }

        // 각 헤더를 순회하며 유효한 IP 찾기
        for (String header : IP_HEADER_CANDIDATES) {
            String ip = request.getHeader(header);
            if (isValidIp(ip)) {
                // X-Forwarded-For는 여러 IP가 쉼표로 구분될 수 있음
                // 예: X-Forwarded-For: client, proxy1, proxy2
                // 첫 번째가 실제 클라이언트 IP
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                log.debug("Client IP found from header {}: {}", header, ip);
                return ip;
            }
        }

        // 헤더에서 찾지 못한 경우 RemoteAddr 사용
        String ip = request.getRemoteAddr();
        log.debug("Client IP from RemoteAddr: {}", ip);
        return ip != null ? ip : "0.0.0.0";
    }

    /**
     * IP 주소가 유효한지 검증합니다.
     *
     * @param ip IP 주소
     * @return 유효하면 true
     */
    private static boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            return false;
        }
        return true;
    }

    /**
     * User-Agent 추출
     *
     * @param request HttpServletRequest
     * @return User-Agent 문자열
     */
    public static String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "Unknown";
        }
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "Unknown";
    }
}
