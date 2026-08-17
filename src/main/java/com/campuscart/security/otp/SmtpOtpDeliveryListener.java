package com.campuscart.security.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Event listener that dispatches outbound OTP delivery messages to SMTP when
 * configured, or records a safe diagnostic message in development mode.
 */
@Component
public class SmtpOtpDeliveryListener {

    private static final Logger log = LoggerFactory.getLogger(SmtpOtpDeliveryListener.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String fromAddress;

    public SmtpOtpDeliveryListener(ObjectProvider<JavaMailSender> mailSenderProvider,
                                   @Value("${mail.from:noreply@campuscart.local}") String fromAddress) {
        this.mailSenderProvider = mailSenderProvider;
        this.fromAddress = fromAddress;
    }

    @EventListener
    public void onOtpDelivery(OtpDeliveryMessage message) {
        if (message.channel() == OtpChannel.EMAIL) {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender != null) {
                try {
                    SimpleMailMessage mail = new SimpleMailMessage();
                    mail.setFrom(fromAddress);
                    mail.setTo(message.destination());
                    mail.setSubject("Your CampusCart Verification Code");
                    mail.setText("Welcome to CampusCart!\n\n"
                            + "Your one-time verification code is: " + message.code() + "\n\n"
                            + "This code will expire in 5 minutes.\n"
                            + "For your security, do not share this code with anyone.");
                    mailSender.send(mail);
                    log.info("OTP email successfully dispatched to destination: {}",
                            maskEmail(message.destination()));
                } catch (Exception e) {
                    log.error("Failed to send OTP email to destination: {}",
                            maskEmail(message.destination()), e);
                }
            } else {
                log.info("Mail sender not configured (MAIL_HOST unset). OTP delivery handled via event for: {}",
                        maskEmail(message.destination()));
            }
        }
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int at = email.indexOf('@');
        return email.substring(0, 1) + "***" + email.substring(at);
    }
}
