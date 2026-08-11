package com.campuscart.chat.safety;

import com.campuscart.common.exception.UnsafeContentException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Fast local guardrail for chat input. It combines high-confidence contact and
 * malicious-content patterns with repetition and abuse signals; a provider-backed
 * classifier can be added behind this boundary for deeper moderation.
 */
@Component
public class ChatContentSafetyService {

    private static final Pattern EMAIL = Pattern.compile(
            "\\b[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern PHONE = Pattern.compile(
            "(?<!\\d)(?:\\+?\\d[\\d ()().-]{7,}\\d)(?!\\d)");
    private static final Pattern LINK_OR_HANDLE = Pattern.compile(
            "(?i)(https?://|www\\.|bit\\.ly/|t\\.me/|@[a-z0-9_.]{3,}|"
                    + "(?:instagram|telegram|whatsapp|snapchat|facebook|discord|signal)\\s*[:@])");
    private static final Pattern SCRIPT_OR_MARKUP = Pattern.compile(
            "(?i)<\\s*(script|iframe|object|embed)|javascript:|data:text/html");
    private static final Pattern REPEATED_CHARACTER = Pattern.compile("(.)\\1{7,}");
    private static final Set<String> ABUSE_SIGNALS = Set.of(
            "idiot", "stupid", "scam", "hate", "moron", "shut-up");

    public void validateText(String content) {
        String normalized = Normalizer.normalize(content == null ? "" : content, Normalizer.Form.NFKC)
                .trim();
        if (normalized.isEmpty()) {
            throw new UnsafeContentException("Message content is required.");
        }
        if (EMAIL.matcher(normalized).find() || PHONE.matcher(normalized).find()
                || LINK_OR_HANDLE.matcher(normalized).find()) {
            throw new UnsafeContentException("Contact details and external contact handles are not allowed in chat.");
        }
        if (SCRIPT_OR_MARKUP.matcher(normalized).find()) {
            throw new UnsafeContentException("Markup and executable content are not allowed in chat.");
        }
        if (REPEATED_CHARACTER.matcher(normalized).find() || hasRepeatedToken(normalized)) {
            throw new UnsafeContentException("Repeated or spam-like content is not allowed.");
        }
        long abuseSignals = Arrays.stream(normalized.toLowerCase(Locale.ROOT).split("[^a-z0-9-]+"))
                .filter(ABUSE_SIGNALS::contains)
                .count();
        if (abuseSignals > 0) {
            throw new UnsafeContentException("Abusive content is not allowed.");
        }
    }

    private boolean hasRepeatedToken(String text) {
        String[] tokens = text.toLowerCase(Locale.ROOT).split("\\s+");
        for (int index = 0; index + 3 < tokens.length; index++) {
            if (tokens[index].equals(tokens[index + 1])
                    && tokens[index].equals(tokens[index + 2])
                    && tokens[index].equals(tokens[index + 3])) {
                return true;
            }
        }
        return false;
    }
}
