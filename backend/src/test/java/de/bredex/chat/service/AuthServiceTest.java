package de.bredex.chat.service;

import de.bredex.chat.dto.AuthResponse;
import de.bredex.chat.dto.LoginRequest;
import de.bredex.chat.dto.RegisterRequest;
import de.bredex.chat.entity.User;
import de.bredex.chat.exception.UsernameAlreadyExistsException;
import de.bredex.chat.repository.UserRepository;
import de.bredex.chat.security.JwtTokenProvider;
import de.bredex.chat.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_success() throws Exception {
        RegisterRequest request = new RegisterRequest("alice", "password123");
        when(userRepository.existsByUsernameIgnoreCase("alice")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, UUID.randomUUID());
            return user;
        });
        when(jwtTokenProvider.generateToken(any(UUID.class), eq("alice"))).thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateUsername_throwsConflict() {
        RegisterRequest request = new RegisterRequest("alice", "password123");
        when(userRepository.existsByUsernameIgnoreCase("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UsernameAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_success() {
        LoginRequest request = new LoginRequest("alice", "password123");
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(userId, "alice", null);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateToken(userId, "alice")).thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
    }

    @Test
    void login_badCredentials_throwsException() {
        LoginRequest request = new LoginRequest("alice", "wrong");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}
