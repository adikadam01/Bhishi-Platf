package com.bhishi.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class CodeGenerator {

    private static final SecureRandom RANDOM    = new SecureRandom();
    private static final String       ALPHA_NUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /** Generates a 6-char alphanumeric group join code e.g. "PLB2K9" */
    public String generateGroupCode() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++)
            sb.append(ALPHA_NUM.charAt(RANDOM.nextInt(ALPHA_NUM.length())));
        return sb.toString();
    }

    /** Generates a numeric OTP of given length e.g. "483920" */
    public String generateOtp(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(RANDOM.nextInt(10));
        return sb.toString();
    }

    /** Generates a hex seed for CONTROLLED_RANDOM payout audit transparency */
    public String generateSeed() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) hex.append(String.format("%02x", b));
        return hex.toString();
    }
}
