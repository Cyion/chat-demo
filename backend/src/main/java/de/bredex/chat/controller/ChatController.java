package de.bredex.chat.controller;

import de.bredex.chat.dto.ChatCreationResult;
import de.bredex.chat.dto.ChatResponse;
import de.bredex.chat.dto.CreateChatRequest;
import de.bredex.chat.security.UserPrincipal;
import de.bredex.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Chats", description = "Chat management endpoints. Messages are not implemented — only chat/participant management.")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    @Operation(summary = "Get all chats for the authenticated user", responses = {
            @ApiResponse(responseCode = "200", description = "List of chats"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ChatResponse>> getChats(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(chatService.getChatsForUser(principal.id()));
    }

    @PostMapping
    @Operation(summary = "Create a new chat with another user",
            description = "Returns existing chat (200) if one already exists between the two users, or creates a new one (201).",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Chat created"),
                    @ApiResponse(responseCode = "200", description = "Chat already exists"),
                    @ApiResponse(responseCode = "400", description = "Cannot chat with yourself"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "Other user not found")
            })
    public ResponseEntity<ChatResponse> createChat(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateChatRequest request) {
        ChatCreationResult result = chatService.createChat(principal.id(), request.otherUsername());
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.chat());
    }
}
