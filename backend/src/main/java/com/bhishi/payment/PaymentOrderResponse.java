package com.bhishi.payment;

import lombok.*;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentOrderResponse {
    private String  paymentId;
    private String  razorpayOrderId;
    private String  razorpayKeyId;
    private double  amount;
    private double  baseAmount;
    private double  penaltyAmount;
    private boolean isLate;
    private String  currency;
    private String  groupName;
    private int     cycleMonth;
    private int     cycleYear;
    private LocalDateTime dueDate;
}
