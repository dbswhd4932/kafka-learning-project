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
public class ScoreUpdateResponse implements Serializable {

    private String message;
    private String userId;
    private Double score;
    private Double delta;      // 증가분 (increment일 때만 사용)
    private Double newScore;   // 새로운 점수 (increment일 때만 사용)

}
