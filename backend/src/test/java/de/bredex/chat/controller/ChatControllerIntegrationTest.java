package de.bredex.chat.controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import de.bredex.chat.dto.CreateChatRequest;
import de.bredex.chat.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ChatControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String aliceToken;
    private String bobToken;

    @BeforeEach
    void setUp() throws Exception {
        aliceToken = registerAndGetToken("alice", "password123");
        bobToken = registerAndGetToken("bob", "password123");
    }

    private String registerAndGetToken(String username, String password) throws Exception {
        RegisterRequest request = new RegisterRequest(username, password);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    @Test
    void createChat_success() throws Exception {
        CreateChatRequest request = new CreateChatRequest("bob");

        mockMvc.perform(post("/api/chats")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.participants").isArray())
                .andExpect(jsonPath("$.participants.length()").value(2));
    }

    @Test
    void createChat_duplicate_returns200() throws Exception {
        CreateChatRequest request = new CreateChatRequest("bob");

        MvcResult first = mockMvc.perform(post("/api/chats")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult second = mockMvc.perform(post("/api/chats")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String firstId = objectMapper.readTree(first.getResponse().getContentAsString()).get("id").asText();
        String secondId = objectMapper.readTree(second.getResponse().getContentAsString()).get("id").asText();
        assertThat(firstId).isEqualTo(secondId);
    }

    @Test
    void createChat_selfChat_returns400() throws Exception {
        CreateChatRequest request = new CreateChatRequest("alice");

        mockMvc.perform(post("/api/chats")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getChats_returnsOnlyOwnChats() throws Exception {
        CreateChatRequest request = new CreateChatRequest("bob");
        mockMvc.perform(post("/api/chats")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/chats")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/chats")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        String charlieToken = registerAndGetToken("charlie", "password123");
        mockMvc.perform(get("/api/chats")
                        .header("Authorization", "Bearer " + charlieToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getChats_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/chats"))
                .andExpect(status().isUnauthorized());
    }
}
