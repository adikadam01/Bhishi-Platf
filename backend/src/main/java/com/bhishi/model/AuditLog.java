package com.bhishi.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "audit_logs")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditLog {
    @Id private String id;
    private String actorId;
    private String actorRole;
    private String action;
    private String targetType;
    private String targetId;
    private String groupId;
    private Map<String, Object> details;
    @CreatedDate private LocalDateTime createdAt;
}
