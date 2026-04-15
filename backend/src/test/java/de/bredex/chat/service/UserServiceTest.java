package de.bredex.chat.service;

import de.bredex.chat.dto.UserSummaryResponse;
import de.bredex.chat.entity.User;
import de.bredex.chat.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

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

    @Test
    void searchUsers_returnsMatchingUsers() {
        User alice = createUser(UUID.randomUUID(), "alice");
        when(userRepository.searchByUsername("ali", PageRequest.of(0, 20)))
                .thenReturn(List.of(alice));

        List<UserSummaryResponse> results = userService.searchUsers("ali");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).username()).isEqualTo("alice");
    }

    @Test
    void searchUsers_noMatch_returnsEmpty() {
        when(userRepository.searchByUsername("xyz", PageRequest.of(0, 20)))
                .thenReturn(List.of());

        List<UserSummaryResponse> results = userService.searchUsers("xyz");

        assertThat(results).isEmpty();
    }
}
