package com.bhishi.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "payments")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Payment {
    @Id private String id;
    private String groupId;
    private String userId;
    private int cycleMonth;
    private int cycleYear;
    private double baseAmount;
    private double penaltyAmount;
    private double totalAmount;
    private PaymentStatus status;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private LocalDateTime dueDate;
    private LocalDateTime paidAt;
    private boolean isLate;
    @CreatedDate private LocalDateTime createdAt;
    @LastModifiedDate private LocalDateTime updatedAt;

    public enum PaymentStatus { PENDING, PAID, LATE, WAIVED }
}
