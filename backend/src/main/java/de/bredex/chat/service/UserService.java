package de.bredex.chat.service;

import de.bredex.chat.dto.UserSummaryResponse;
import de.bredex.chat.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private static final int MAX_SEARCH_RESULTS = 20;

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserSummaryResponse> searchUsers(String query) {
        return userRepository.searchByUsername(query, PageRequest.of(0, MAX_SEARCH_RESULTS)).stream()
                .map(u -> new UserSummaryResponse(u.getId(), u.getUsername()))
                .toList();
    }
}
