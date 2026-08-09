package com.campuscart.security.otp;

/** Adapter boundary for email/SMS providers. Implementations must never log the code. */
public interface OtpDeliveryGateway {

    void deliver(OtpDeliveryMessage message);
}
