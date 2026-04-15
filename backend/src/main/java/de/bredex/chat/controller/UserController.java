package de.bredex.chat.controller;

import de.bredex.chat.dto.UserSummaryResponse;
import de.bredex.chat.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Users", description = "User search endpoints. Rate limiting is recommended for production but not implemented.")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/search")
    @Operation(summary = "Search users by username",
            description = "Case-insensitive partial match (contains), max 20 results. Includes the current user in results.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of matching users"),
                    @ApiResponse(responseCode = "400", description = "Username query parameter is required"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    public ResponseEntity<List<UserSummaryResponse>> searchUsers(@RequestParam String username) {
        if (username.isBlank()) {
            throw new IllegalArgumentException("Username search query must not be blank");
        }
        return ResponseEntity.ok(userService.searchUsers(username));
    }
}
