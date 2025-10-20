package com.example.hightraffic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 커서 기반 페이지네이션 응답 DTO (무한 스크롤용)
 *
 * Offset 기반 vs Cursor 기반:
 * - Offset: 페이지 번호를 사용, 중간 페이지 접근 가능하지만 데이터가 추가/삭제되면 중복/누락 발생 가능
 * - Cursor: 마지막 항목의 ID를 기준으로 조회, 일관성 있는 결과를 보장하지만 특정 페이지로 이동 불가
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPageResponse<T> {

    private List<T> content;
    private Long nextCursor;
    private boolean hasNext;
    private int size;

    public static <T> CursorPageResponse<T> of(List<T> content, Long nextCursor, boolean hasNext) {
        return CursorPageResponse.<T>builder()
                .content(content)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .size(content.size())
                .build();
    }
}
