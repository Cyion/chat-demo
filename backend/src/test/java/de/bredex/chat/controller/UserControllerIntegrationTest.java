package de.bredex.chat.controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String aliceToken;

    @BeforeEach
    void setUp() throws Exception {
        aliceToken = registerAndGetToken("alice", "password123");
        registerAndGetToken("bob", "password123");
        registerAndGetToken("charlie", "password123");
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
    void searchUsers_returnsMatches() throws Exception {
        mockMvc.perform(get("/api/users/search")
                        .param("username", "ali")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("alice"));
    }

    @Test
    void searchUsers_partialMatch() throws Exception {
        mockMvc.perform(get("/api/users/search")
                        .param("username", "l")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)); // alice, charlie
    }

    @Test
    void searchUsers_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/users/search")
                        .param("username", "ali"))
                .andExpect(status().isUnauthorized());
    }
}
