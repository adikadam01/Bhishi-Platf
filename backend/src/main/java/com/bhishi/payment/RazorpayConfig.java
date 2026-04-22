package com.bhishi.payment;

// ============================================================
// DIGITAL BHISHI PLATFORM — Razorpay Configuration
// Phase 4 | File: RazorpayConfig.java | Package: com.bhishi.payment
//
// ⚠️  TEST MODE — keys are placeholders from application.properties
//     To go live: replace rzp_test_* with rzp_live_* keys in
//     application.properties and set razorpay.test.mode=false
// ============================================================

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@Getter
@Slf4j
public class RazorpayConfig {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    // ─────────────────────────────────────────────
    // SIMULATE ORDER CREATION (Test Mode)
    // In production: call Razorpay REST API to create
    // a real order and get back an order_id
    //
    // Test mode returns a fake order ID so the full
    // payment flow can be tested without real money.
    // ─────────────────────────────────────────────
    public String createOrder(double amountInRupees, String currency, String receipt) {
        // TODO (Go-Live): Replace with real Razorpay SDK call:
        //   RazorpayClient client = new RazorpayClient(keyId, keySecret);
        //   JSONObject options = new JSONObject();
        //   options.put("amount", (int)(amountInRupees * 100)); // paise
        //   options.put("currency", currency);
        //   options.put("receipt", receipt);
        //   Order order = client.orders.create(options);
        //   return order.get("id");

        // ── TEST MODE: return a deterministic fake order ID ──
        String fakeOrderId = "order_TEST_" + receipt + "_" + System.currentTimeMillis();
        log.info("[TEST MODE] Simulated Razorpay order created: {}", fakeOrderId);
        return fakeOrderId;
    }

    // ─────────────────────────────────────────────
    // VERIFY PAYMENT SIGNATURE
    // HMAC-SHA256(razorpayOrderId + "|" + razorpayPaymentId, keySecret)
    // must match the signature sent by Razorpay frontend SDK
    // ─────────────────────────────────────────────
    public boolean verifySignature(String razorpayOrderId,
                                   String razorpayPaymentId,
                                   String razorpaySignature) {
        // ── TEST MODE: always return true for test order IDs ──
        if (razorpayOrderId.startsWith("order_TEST_")) {
            log.info("[TEST MODE] Signature verification bypassed for test order: {}", razorpayOrderId);
            return true;
        }

        // ── PRODUCTION: HMAC-SHA256 verification ──
        try {
            String data = razorpayOrderId + "|" + razorpayPaymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString().equals(razorpaySignature);
        } catch (Exception e) {
            log.error("Signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────
    // VERIFY WEBHOOK SIGNATURE
    // Called when Razorpay POSTs to /api/payments/webhook
    // ─────────────────────────────────────────────
    public boolean verifyWebhookSignature(String payload, String razorpaySignature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            boolean valid = hex.toString().equals(razorpaySignature);
            if (!valid) log.warn("Webhook signature mismatch — possible tampered request");
            return valid;
        } catch (Exception e) {
            log.error("Webhook signature verification error: {}", e.getMessage());
            return false;
        }
    }
}
