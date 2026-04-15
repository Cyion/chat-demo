package de.bredex.chat.security;

import de.bredex.chat.repository.ChatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final ChatRepository chatRepository;

    public WebSocketAuthInterceptor(JwtTokenProvider jwtTokenProvider, ChatRepository chatRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.chatRepository = chatRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);
            if (token != null && jwtTokenProvider.validateToken(token)) {
                UUID userId = jwtTokenProvider.getUserIdFromToken(token);
                String username = jwtTokenProvider.getUsernameFromToken(token);
                UserPrincipal principal = new UserPrincipal(userId, username, null);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null, List.of());
                accessor.setUser(auth);
            } else {
                throw new AccessDeniedException("Invalid or missing JWT token");
            }
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith("/topic/chat/")) {
                UsernamePasswordAuthenticationToken auth =
                        (UsernamePasswordAuthenticationToken) accessor.getUser();
                if (auth == null) {
                    throw new AccessDeniedException("Not authenticated");
                }
                UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
                String chatIdStr = destination.substring("/topic/chat/".length());
                try {
                    UUID chatId = UUID.fromString(chatIdStr);
                    var chat = chatRepository.findByIdWithParticipants(chatId);
                    if (chat.isEmpty() || chat.get().getParticipants().stream()
                            .noneMatch(u -> u.getId().equals(principal.id()))) {
                        throw new AccessDeniedException("Not a participant of this chat");
                    }
                } catch (IllegalArgumentException e) {
                    throw new AccessDeniedException("Invalid chat ID");
                }
            }
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String bearerToken = authHeaders.getFirst();
            if (bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7);
            }
        }
        return null;
    }
}
