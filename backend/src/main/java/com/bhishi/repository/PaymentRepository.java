package com.bhishi.repository;

import com.bhishi.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {
    List<Payment> findByGroupIdAndCycleMonthAndCycleYear(String groupId, int month, int year);
    Optional<Payment> findByGroupIdAndUserIdAndCycleMonthAndCycleYear(
            String groupId, String userId, int month, int year);
    List<Payment> findByUserId(String userId);
    List<Payment> findByGroupId(String groupId);
    List<Payment> findByGroupIdAndStatus(String groupId, Payment.PaymentStatus status);
    Optional<Payment> findByRazorpayOrderId(String orderId);

    @Query("{ 'groupId': ?0, 'cycleMonth': ?1, 'cycleYear': ?2, 'status': 'PAID' }")
    List<Payment> findPaidPaymentsByCycle(String groupId, int month, int year);

    @Query(value = "{ 'groupId': ?0, 'cycleMonth': ?1, 'cycleYear': ?2, 'status': 'PAID' }", count = true)
    long countPaidByCycle(String groupId, int month, int year);
}
