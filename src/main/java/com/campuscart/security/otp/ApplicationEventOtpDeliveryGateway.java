package com.campuscart.security.otp;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Publishes an in-process delivery event for an email/SMS adapter. */
@Component
public class ApplicationEventOtpDeliveryGateway implements OtpDeliveryGateway {

    private final ApplicationEventPublisher eventPublisher;

    public ApplicationEventOtpDeliveryGateway(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void deliver(OtpDeliveryMessage message) {
        eventPublisher.publishEvent(message);
    }
}
