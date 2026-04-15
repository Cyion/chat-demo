package de.bredex.chat.dto;

import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String username
) {}
