package de.bredex.chat.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatResponse(
        UUID id,
        List<UserSummaryResponse> participants,
        Instant createdAt
) {}
