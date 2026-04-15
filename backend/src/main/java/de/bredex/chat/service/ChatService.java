package de.bredex.chat.service;

import de.bredex.chat.dto.ChatCreationResult;
import de.bredex.chat.dto.ChatResponse;
import de.bredex.chat.dto.MessageResponse;
import de.bredex.chat.dto.UserSummaryResponse;
import de.bredex.chat.entity.Chat;
import de.bredex.chat.entity.User;
import de.bredex.chat.exception.ResourceNotFoundException;
import de.bredex.chat.repository.ChatRepository;
import de.bredex.chat.repository.MessageRepository;
import de.bredex.chat.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Stream;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    public ChatService(ChatRepository chatRepository, UserRepository userRepository,
                       MessageRepository messageRepository) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional(readOnly = true)
    public List<ChatResponse> getChatsForUser(UUID userId) {
        return chatRepository.findChatsByUserId(userId).stream()
                .map(this::toChatResponse)
                .toList();
    }

    @Transactional
    public ChatCreationResult createChat(UUID currentUserId, String otherUsername) {
        User otherUser = userRepository.findByUsernameIgnoreCase(otherUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", otherUsername));

        if (otherUser.getId().equals(currentUserId)) {
            throw new IllegalArgumentException("Cannot create a chat with yourself");
        }

        String participantHash = computeParticipantHash(currentUserId, otherUser.getId());

        Optional<Chat> existingChat = chatRepository.findByParticipantHash(participantHash);
        if (existingChat.isPresent()) {
            return new ChatCreationResult(toChatResponse(existingChat.get()), false);
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));

        Chat chat = new Chat(participantHash, new HashSet<>(Set.of(currentUser, otherUser)));
        try {
            chat = chatRepository.save(chat);
            log.info("Created chat {} between users {} and {}", chat.getId(), currentUserId, otherUser.getId());
            return new ChatCreationResult(toChatResponse(chat), true);
        } catch (DataIntegrityViolationException e) {
            Chat concurrentChat = chatRepository.findByParticipantHash(participantHash)
                    .orElseThrow(() -> new RuntimeException("Unexpected error during concurrent chat creation"));
            return new ChatCreationResult(toChatResponse(concurrentChat), false);
        }
    }

    private String computeParticipantHash(UUID userId1, UUID userId2) {
        return Stream.of(userId1.toString(), userId2.toString())
                .sorted()
                .reduce((a, b) -> a + "_" + b)
                .orElseThrow();
    }

    private ChatResponse toChatResponse(Chat chat) {
        List<UserSummaryResponse> participants = chat.getParticipants().stream()
                .map(u -> new UserSummaryResponse(u.getId(), u.getUsername()))
                .toList();
        MessageResponse lastMessage = messageRepository.findLatestByChatId(chat.getId())
                .map(m -> new MessageResponse(
                        m.getId(), m.getChat().getId(), m.getSender().getId(),
                        m.getSender().getUsername(), m.getContent(), m.getCreatedAt()))
                .orElse(null);
        return new ChatResponse(chat.getId(), participants, chat.getCreatedAt(), lastMessage);
    }
}
