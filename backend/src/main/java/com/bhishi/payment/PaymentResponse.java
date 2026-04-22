package com.bhishi.payment;

import lombok.*;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentResponse {
    private String  id;
    private String  groupId;
    private String  groupName;
    private String  userId;
    private String  memberName;
    private int     cycleMonth;
    private int     cycleYear;
    private double  baseAmount;
    private double  penaltyAmount;
    private double  totalAmount;
    private String  status;
    private boolean isLate;
    private String  razorpayOrderId;
    private String  razorpayPaymentId;
    private LocalDateTime dueDate;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
