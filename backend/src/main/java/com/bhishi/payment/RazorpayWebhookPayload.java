package com.bhishi.payment;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class RazorpayWebhookPayload {
    private String        entity;
    private RazorpayEvent event;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RazorpayEvent {
        private RazorpayPaymentEntity payment;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RazorpayPaymentEntity {
        private RazorpayPaymentItem entity;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RazorpayPaymentItem {
        private String id;
        private String order_id;
        private String status;
        private long   amount;
        private String currency;
    }
}
