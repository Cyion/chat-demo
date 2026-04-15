package de.bredex.chat.dto;

public record ChatCreationResult(
        ChatResponse chat,
        boolean created
) {}
