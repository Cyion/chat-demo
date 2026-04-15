package de.bredex.chat.service;

import de.bredex.chat.dto.AuthResponse;
import de.bredex.chat.dto.LoginRequest;
import de.bredex.chat.dto.RegisterRequest;
import de.bredex.chat.entity.User;
import de.bredex.chat.exception.UsernameAlreadyExistsException;
import de.bredex.chat.repository.UserRepository;
import de.bredex.chat.security.JwtTokenProvider;
import de.bredex.chat.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new UsernameAlreadyExistsException(request.username());
        }

        User user = new User(request.username(), passwordEncoder.encode(request.password()));
        user = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());
        return new AuthResponse(token, jwtTokenProvider.getExpirationMs());
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(principal.id(), principal.getUsername());
        return new AuthResponse(token, jwtTokenProvider.getExpirationMs());
    }
}
