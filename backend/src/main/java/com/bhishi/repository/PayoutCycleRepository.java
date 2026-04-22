package com.bhishi.repository;

import com.bhishi.model.PayoutCycle;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayoutCycleRepository extends MongoRepository<PayoutCycle, String> {
    List<PayoutCycle> findByGroupIdOrderByCycleNumberDesc(String groupId);
    Optional<PayoutCycle> findByGroupIdAndCycleNumber(String groupId, int cycleNumber);
    boolean existsByGroupIdAndWinnerId(String groupId, String winnerId);
    List<PayoutCycle> findByGroupIdAndStatus(String groupId, PayoutCycle.CycleStatus status);
}
