package de.bredex.chat.service;

import de.bredex.chat.dto.MessageResponse;
import de.bredex.chat.entity.Chat;
import de.bredex.chat.entity.Message;
import de.bredex.chat.entity.User;
import de.bredex.chat.exception.ResourceNotFoundException;
import de.bredex.chat.repository.ChatRepository;
import de.bredex.chat.repository.MessageRepository;
import de.bredex.chat.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageService(MessageRepository messageRepository, ChatRepository chatRepository,
                          UserRepository userRepository, SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(UUID chatId, UUID userId, int page, int size) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat", "id", chatId.toString()));

        verifyParticipant(chat, userId);

        return messageRepository.findByChatId(chatId, PageRequest.of(page, size)).stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional
    public MessageResponse sendMessage(UUID chatId, UUID senderId, String content) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat", "id", chatId.toString()));

        verifyParticipant(chat, senderId);

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", senderId.toString()));

        Message message = new Message(chat, sender, content);
        message = messageRepository.save(message);

        log.info("Message {} sent in chat {} by user {}", message.getId(), chatId, senderId);
        MessageResponse response = toMessageResponse(message);
        messagingTemplate.convertAndSend("/topic/chat/" + chatId, response);
        return response;
    }

    private void verifyParticipant(Chat chat, UUID userId) {
        boolean isParticipant = chat.getParticipants().stream()
                .anyMatch(u -> u.getId().equals(userId));
        if (!isParticipant) {
            throw new org.springframework.security.access.AccessDeniedException("You are not a participant of this chat");
        }
    }

    MessageResponse toMessageResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getChat().getId(),
                message.getSender().getId(),
                message.getSender().getUsername(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
