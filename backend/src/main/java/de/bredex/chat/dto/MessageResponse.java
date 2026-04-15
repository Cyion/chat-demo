package de.bredex.chat.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID chatId,
        UUID senderId,
        String senderUsername,
        String content,
        Instant createdAt
) {
}
