package com.example.redis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankingResponse implements Serializable {

    private Long rank;        // 순위 (1위, 2위, ...)
    private String userId;    // 사용자 ID
    private Double score;     // 점수

}
