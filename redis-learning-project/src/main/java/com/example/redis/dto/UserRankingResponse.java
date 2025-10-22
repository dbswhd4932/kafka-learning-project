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
public class UserRankingResponse implements Serializable {

    private RankingResponse data;
    private Boolean found;
    private String message;  // found가 false일 때 메시지

}
