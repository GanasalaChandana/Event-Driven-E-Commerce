package com.shopflow.order;

import com.shopflow.order.payment.StripeSignature;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class StripeSignatureTest {

    private static final String SECRET = "whsec_test_secret";
    private static final byte[] PAYLOAD = "{\"type\":\"checkout.session.completed\"}".getBytes(StandardCharsets.UTF_8);
    private static final Instant NOW = Instant.ofEpochSecond(1_800_000_000L);
    private static final Duration TOLERANCE = Duration.ofMinutes(5);

    private static String header(long t, byte[] payload, String secret) {
        return "t=" + t + ",v1=" + StripeSignature.hmac(t, payload, secret);
    }

    @Test
    void validSignature_isAccepted() {
        assertThat(StripeSignature.verify(PAYLOAD, header(NOW.getEpochSecond(), PAYLOAD, SECRET), SECRET, TOLERANCE, NOW)).isTrue();
    }

    @Test
    void anyMatchingV1_isAccepted() {
        long t = NOW.getEpochSecond();
        String h = "t=" + t + ",v1=deadbeef,v1=" + StripeSignature.hmac(t, PAYLOAD, SECRET);
        assertThat(StripeSignature.verify(PAYLOAD, h, SECRET, TOLERANCE, NOW)).isTrue();
    }

    @Test
    void wrongSecret_isRejected() {
        assertThat(StripeSignature.verify(PAYLOAD, header(NOW.getEpochSecond(), PAYLOAD, "other"), SECRET, TOLERANCE, NOW)).isFalse();
    }

    @Test
    void tamperedPayload_isRejected() {
        String h = header(NOW.getEpochSecond(), PAYLOAD, SECRET);
        byte[] tampered = "{\"type\":\"evil\"}".getBytes(StandardCharsets.UTF_8);
        assertThat(StripeSignature.verify(tampered, h, SECRET, TOLERANCE, NOW)).isFalse();
    }

    @Test
    void staleTimestamp_isRejected() {
        long old = NOW.minusSeconds(3600).getEpochSecond();
        assertThat(StripeSignature.verify(PAYLOAD, header(old, PAYLOAD, SECRET), SECRET, TOLERANCE, NOW)).isFalse();
    }

    @Test
    void missingOrMalformedHeader_isRejected() {
        assertThat(StripeSignature.verify(PAYLOAD, null, SECRET, TOLERANCE, NOW)).isFalse();
        assertThat(StripeSignature.verify(PAYLOAD, "garbage", SECRET, TOLERANCE, NOW)).isFalse();
        assertThat(StripeSignature.verify(PAYLOAD, "t=abc,v1=00", SECRET, TOLERANCE, NOW)).isFalse();
        assertThat(StripeSignature.verify(PAYLOAD, "v1=00", SECRET, TOLERANCE, NOW)).isFalse();
    }
}
