package com.shopflow.order.payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/** Verifies the Stripe-Signature header: "t=<unix time>,v1=<hex hmac-sha256 of "t.payload">". */
public final class StripeSignature {

    private StripeSignature() {}

    public static boolean verify(byte[] payload, String header, String secret, Duration tolerance, Instant now) {
        if (payload == null || header == null || secret == null || secret.isEmpty()) {
            return false;
        }

        long timestamp = -1;
        java.util.List<String> signatures = new java.util.ArrayList<>();
        for (String part : header.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length != 2) continue;
            if (kv[0].equals("t")) {
                try {
                    timestamp = Long.parseLong(kv[1]);
                } catch (NumberFormatException e) {
                    return false;
                }
            } else if (kv[0].equals("v1")) {
                signatures.add(kv[1]);
            }
        }
        if (timestamp < 0 || signatures.isEmpty()) {
            return false;
        }
        if (Math.abs(now.getEpochSecond() - timestamp) > tolerance.getSeconds()) {
            return false;
        }

        byte[] expected = hmac(timestamp, payload, secret).getBytes(StandardCharsets.UTF_8);
        for (String candidate : signatures) {
            if (MessageDigest.isEqual(expected, candidate.getBytes(StandardCharsets.UTF_8))) {
                return true;
            }
        }
        return false;
    }

    /** Public so tests can build a valid header. */
    public static String hmac(long timestamp, byte[] payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(mac.doFinal(payload));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }
}
