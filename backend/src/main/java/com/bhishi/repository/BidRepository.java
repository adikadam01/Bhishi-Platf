package com.bhishi.repository;

import com.bhishi.model.Bid;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends MongoRepository<Bid, String> {
    List<Bid> findByCycleIdOrderByBidAmountAsc(String cycleId);
    Optional<Bid> findByCycleIdAndUserId(String cycleId, String userId);
    List<Bid> findByGroupId(String groupId);
    boolean existsByCycleIdAndUserId(String cycleId, String userId);
}
