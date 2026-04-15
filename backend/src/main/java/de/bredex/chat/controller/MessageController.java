package de.bredex.chat.controller;

import de.bredex.chat.dto.MessageResponse;
import de.bredex.chat.dto.SendMessageRequest;
import de.bredex.chat.security.UserPrincipal;
import de.bredex.chat.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chats/{chatId}/messages")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Messages", description = "Send and retrieve chat messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    @Operation(summary = "Get messages for a chat", responses = {
            @ApiResponse(responseCode = "200", description = "List of messages"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not a participant"),
            @ApiResponse(responseCode = "404", description = "Chat not found")
    })
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable UUID chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(messageService.getMessages(chatId, principal.id(), page, size));
    }

    @PostMapping
    @Operation(summary = "Send a message in a chat", responses = {
            @ApiResponse(responseCode = "201", description = "Message sent"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not a participant"),
            @ApiResponse(responseCode = "404", description = "Chat not found")
    })
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable UUID chatId,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        MessageResponse message = messageService.sendMessage(chatId, principal.id(), request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }
}
