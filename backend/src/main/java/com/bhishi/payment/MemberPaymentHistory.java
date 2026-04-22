package com.bhishi.payment;

import lombok.*;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MemberPaymentHistory {
    private String userId;
    private String memberName;
    private int    totalPaid;
    private int    totalPending;
    private int    totalLate;
    private double totalAmountPaid;
    private List<PaymentResponse> payments;
}
