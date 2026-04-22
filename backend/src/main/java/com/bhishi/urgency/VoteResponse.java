package com.bhishi.urgency;

import lombok.*;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class VoteResponse {
    private String userId;
    private String memberName;
    private String vote;
    private LocalDateTime votedAt;
}
