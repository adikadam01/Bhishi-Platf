package com.bhishi.payment;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CyclePaymentSummary {
    private String  groupId;
    private String  groupName;
    private int     cycleMonth;
    private int     cycleYear;
    private int     totalMembers;
    private int     paidCount;
    private int     pendingCount;
    private int     lateCount;
    private double  totalCollected;
    private double  totalExpected;
    private LocalDateTime dueDate;
    private List<PaymentResponse> payments;
}
