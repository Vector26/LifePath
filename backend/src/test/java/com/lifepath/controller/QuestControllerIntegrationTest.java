package com.lifepath.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifepath.dto.CreateQuestRequest;
import com.lifepath.dto.RegisterRequest;
import com.lifepath.model.enums.PriorityLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QuestControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("questuser-" + UUID.randomUUID() + "@test.com");
        reg.setPassword("password123");
        reg.setTimezone("UTC");
        var regResult = mvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();
        var body = objectMapper.readTree(regResult.getResponse().getContentAsString());
        accessToken = body.get("access_token").asText();
    }

    @Nested
    @DisplayName("POST /quests")
    class Create {

        @Test
        void createsQuest() throws Exception {
            CreateQuestRequest req = new CreateQuestRequest();
            req.setTitle("My first quest");
            req.setNotes("details");
            req.setPriorityLevel(PriorityLevel.P1);

            mvc.perform(post("/quests")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("My first quest"))
                    .andExpect(jsonPath("$.priority_level").value("P1"))
                    .andExpect(jsonPath("$.status").value("pending"));
        }
    }

    @Nested
    @DisplayName("GET /quests")
    class List {

        @Test
        void returnsQuestsForUser() throws Exception {
            mvc.perform(get("/quests")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        void requiresAuth() throws Exception {
            mvc.perform(get("/quests"))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("GET /quests/current")
    class Current {

        @Test
        void returns204WhenNoEligibleQuest() throws Exception {
            mvc.perform(get("/quests/current")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNoContent());
        }
    }
}
