package de.bredex.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateChatRequest(
        @NotBlank(message = "Other username is required")
        String otherUsername
) {}
