package com.campuscart.security.otp;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;

class WhatsAppOtpDeliveryListenerTest {

    @Test
    void ignoresNonPhoneChannels() {
        WhatsAppOtpDeliveryListener listener = new WhatsAppOtpDeliveryListener("test_key", "", "");
        OtpDeliveryMessage emailMessage = new OtpDeliveryMessage(
                OtpChannel.EMAIL,
                "student@bennett.edu.in",
                "123456",
                Instant.now().plusSeconds(300));

        assertThatCode(() -> listener.onOtpDelivery(emailMessage)).doesNotThrowAnyException();
    }

    @Test
    void handlesPhoneChannelGracefullyWithoutThrowingWhenKeyIsConfigured() {
        WhatsAppOtpDeliveryListener listener = new WhatsAppOtpDeliveryListener("", "", "");
        OtpDeliveryMessage phoneMessage = new OtpDeliveryMessage(
                OtpChannel.PHONE,
                "+919876543210",
                "654321",
                Instant.now().plusSeconds(300));

        assertThatCode(() -> listener.onOtpDelivery(phoneMessage)).doesNotThrowAnyException();
    }
}
