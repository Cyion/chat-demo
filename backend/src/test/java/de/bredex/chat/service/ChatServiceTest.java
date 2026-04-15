package de.bredex.chat.service;

import de.bredex.chat.dto.ChatCreationResult;
import de.bredex.chat.dto.ChatResponse;
import de.bredex.chat.entity.Chat;
import de.bredex.chat.entity.User;
import de.bredex.chat.exception.ResourceNotFoundException;
import de.bredex.chat.repository.ChatRepository;
import de.bredex.chat.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatService chatService;

    private User createUser(UUID id, String username) {
        User user = new User(username, "hashed");
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return user;
    }

    private Chat createChat(UUID id, String hash, Set<User> participants) {
        Chat chat = new Chat(hash, participants);
        try {
            var idField = Chat.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(chat, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return chat;
    }

    @Test
    void createChat_success() throws Exception {
        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();
        User alice = createUser(aliceId, "alice");
        User bob = createUser(bobId, "bob");

        when(userRepository.findByUsernameIgnoreCase("bob")).thenReturn(Optional.of(bob));
        when(userRepository.findById(aliceId)).thenReturn(Optional.of(alice));
        when(chatRepository.findByParticipantHash(any())).thenReturn(Optional.empty());
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> {
            Chat chat = invocation.getArgument(0);
            var idField = Chat.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(chat, UUID.randomUUID());
            return chat;
        });

        ChatCreationResult result = chatService.createChat(aliceId, "bob");

        assertThat(result.created()).isTrue();
        assertThat(result.chat().participants()).hasSize(2);
        verify(chatRepository).save(any(Chat.class));
    }

    @Test
    void createChat_alreadyExists_returnsExisting() {
        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();
        User alice = createUser(aliceId, "alice");
        User bob = createUser(bobId, "bob");
        Chat existingChat = createChat(UUID.randomUUID(), "hash", new HashSet<>(Set.of(alice, bob)));

        when(userRepository.findByUsernameIgnoreCase("bob")).thenReturn(Optional.of(bob));
        when(chatRepository.findByParticipantHash(any())).thenReturn(Optional.of(existingChat));

        ChatCreationResult result = chatService.createChat(aliceId, "bob");

        assertThat(result.created()).isFalse();
        verify(chatRepository, never()).save(any());
    }

    @Test
    void createChat_selfChat_throwsException() {
        UUID aliceId = UUID.randomUUID();
        User alice = createUser(aliceId, "alice");

        when(userRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(alice));

        assertThatThrownBy(() -> chatService.createChat(aliceId, "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("yourself");
    }

    @Test
    void createChat_userNotFound_throwsException() {
        UUID aliceId = UUID.randomUUID();
        when(userRepository.findByUsernameIgnoreCase("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.createChat(aliceId, "nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getChatsForUser_returnsOnlyUserChats() {
        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();
        User alice = createUser(aliceId, "alice");
        User bob = createUser(bobId, "bob");
        Chat chat = createChat(UUID.randomUUID(), "hash", new HashSet<>(Set.of(alice, bob)));

        when(chatRepository.findChatsByUserId(aliceId)).thenReturn(List.of(chat));

        List<ChatResponse> chats = chatService.getChatsForUser(aliceId);

        assertThat(chats).hasSize(1);
        assertThat(chats.get(0).participants()).hasSize(2);
    }
}
