package com.bhishi.repository;

import com.bhishi.model.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    List<AuditLog> findByGroupIdOrderByCreatedAtDesc(String groupId);
    List<AuditLog> findByActorId(String actorId);
    List<AuditLog> findByAction(String action);
}
