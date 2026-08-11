package com.campuscart.chat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campuscart.chat.safety.ChatContentSafetyService;
import org.junit.jupiter.api.Test;

class ChatSafetyServiceTest {

    private final ChatContentSafetyService safety = new ChatContentSafetyService();

    @Test
    void blocksEmailAndExternalContactHandles() {
        assertThatThrownBy(() -> safety.validateText("email me at person@example.com"))
                .isInstanceOf(com.campuscart.common.exception.UnsafeContentException.class);
        assertThatThrownBy(() -> safety.validateText("message me on instagram: campuscart"))
                .isInstanceOf(com.campuscart.common.exception.UnsafeContentException.class);
    }

    @Test
    void blocksRepetitionAndAbuseSignalsButAllowsNormalText() {
        assertThatThrownBy(() -> safety.validateText("hello hello hello hello"))
                .isInstanceOf(com.campuscart.common.exception.UnsafeContentException.class);
        assertThatThrownBy(() -> safety.validateText("you are an idiot"))
                .isInstanceOf(com.campuscart.common.exception.UnsafeContentException.class);
        safety.validateText("Is the calculator still available?");
    }

    @Test
    void blocksOverlyLongMessages() {
        assertThatThrownBy(() -> safety.validateText("a".repeat(2_001)))
                .isInstanceOf(com.campuscart.common.exception.UnsafeContentException.class);
    }
}
