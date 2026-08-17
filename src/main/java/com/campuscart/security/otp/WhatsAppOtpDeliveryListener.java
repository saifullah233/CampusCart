package com.campuscart.security.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Event listener that dispatches outbound OTP delivery messages to WhatsApp
 * for the {@link OtpChannel#PHONE} channel using the Fast2SMS WhatsApp API.
 *
 * <p>Preserves all security guarantees: secrets are sourced strictly from environment
 * configuration, phone numbers are masked in logs, and raw OTP codes are never logged.</p>
 */
@Component
public class WhatsAppOtpDeliveryListener {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppOtpDeliveryListener.class);

    private final HttpClient httpClient;
    private final String fast2smsApiKey;
    private final String whatsappMessageId;
    private final String whatsappPhoneNumberId;

    public WhatsAppOtpDeliveryListener(
            @Value("${fast2sms.api-key:${FAST2SMS_API_KEY:}}") String fast2smsApiKey,
            @Value("${fast2sms.whatsapp-message-id:${FAST2SMS_WHATSAPP_MESSAGE_ID:}}") String whatsappMessageId,
            @Value("${fast2sms.whatsapp-phone-number-id:${FAST2SMS_WHATSAPP_PHONE_NUMBER_ID:}}") String whatsappPhoneNumberId) {

        this.fast2smsApiKey = fast2smsApiKey != null ? fast2smsApiKey.trim() : "";
        this.whatsappMessageId = whatsappMessageId != null ? whatsappMessageId.trim() : "";
        this.whatsappPhoneNumberId = whatsappPhoneNumberId != null ? whatsappPhoneNumberId.trim() : "";

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @EventListener
    public void onOtpDelivery(OtpDeliveryMessage message) {
        if (message.channel() != OtpChannel.PHONE) {
            return;
        }

        String destination = message.destination();
        String code = message.code();
        String masked = maskPhone(destination);

        if (fast2smsApiKey.isEmpty()) {
            log.warn("Outbound WhatsApp OTP delivery: FAST2SMS_API_KEY is unset in .env. Real WhatsApp OTP not dispatched to destination: {}", masked);
            return;
        }

        sendViaFast2SmsWhatsApp(destination, code, masked);
    }

    private void sendViaFast2SmsWhatsApp(String destination, String code, String masked) {
        try {
            // Normalize Indian mobile numbers to 10-digit format for Fast2SMS
            String rawDigits = destination.replaceAll("[^0-9]", "").trim();
            String tenDigitPhone = rawDigits.length() >= 10
                    ? rawDigits.substring(rawDigits.length() - 10)
                    : rawDigits;

            StringBuilder urlBuilder = new StringBuilder("https://www.fast2sms.com/dev/whatsapp");
            urlBuilder.append("?numbers=").append(URLEncoder.encode(tenDigitPhone, StandardCharsets.UTF_8))
                    .append("&variables_values=").append(URLEncoder.encode(code, StandardCharsets.UTF_8));

            if (!whatsappMessageId.isEmpty()) {
                urlBuilder.append("&message_id=").append(URLEncoder.encode(whatsappMessageId, StandardCharsets.UTF_8));
            }
            if (!whatsappPhoneNumberId.isEmpty()) {
                urlBuilder.append("&phone_number_id=").append(URLEncoder.encode(whatsappPhoneNumberId, StandardCharsets.UTF_8));
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlBuilder.toString()))
                    .timeout(Duration.ofSeconds(15))
                    .header("authorization", fast2smsApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Fast2SMS WhatsApp OTP successfully dispatched to destination: {}", masked);
            } else {
                log.error("Fast2SMS WhatsApp dispatch returned status {}: destination: {}, response: {}",
                        response.statusCode(), masked, response.body());
            }
        } catch (Exception e) {
            log.error("Failed to send Fast2SMS WhatsApp OTP to destination: {}", masked, e);
        }
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "***";
        }
        return "***" + phone.substring(phone.length() - 4);
    }
}
