package com.bhishi.repository;

import com.bhishi.model.UrgencyRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrgencyRequestRepository extends MongoRepository<UrgencyRequest, String> {
    List<UrgencyRequest> findByGroupId(String groupId);
    List<UrgencyRequest> findByGroupIdAndStatus(String groupId, UrgencyRequest.UrgencyStatus status);
    Optional<UrgencyRequest> findByGroupIdAndRequestedByUserIdAndStatus(
            String groupId, String userId, UrgencyRequest.UrgencyStatus status);

    @Query("{ 'groupId': ?0, 'votingDeadline': { $lt: ?1 }, 'status': 'PENDING' }")
    List<UrgencyRequest> findExpiredRequests(String groupId, LocalDateTime now);
}
