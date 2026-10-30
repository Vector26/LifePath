package com.lifepath.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifepath.dto.LoginRequest;
import com.lifepath.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Nested
    @DisplayName("POST /auth/register")
    class Register {

        @Test
        void createsUserAndReturnsTokens() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("newuser-" + java.util.UUID.randomUUID() + "@test.com");
            req.setPassword("password123");
            req.setDisplayName("Test User");
            req.setTimezone("UTC");

            mvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.user.email").value(req.getEmail()))
                    .andExpect(jsonPath("$.access_token").exists())
                    .andExpect(jsonPath("$.refresh_token").exists());
        }

        @Test
        void rejectsInvalidEmail() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("not-an-email");
            req.setPassword("password123");
            req.setTimezone("UTC");

            mvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        @Test
        void returnsTokensForValidCredentials() throws Exception {
            RegisterRequest reg = new RegisterRequest();
            reg.setEmail("loginuser-" + java.util.UUID.randomUUID() + "@test.com");
            reg.setPassword("password123");
            reg.setTimezone("UTC");
            mvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(reg)));

            LoginRequest req = new LoginRequest();
            req.setEmail(reg.getEmail());
            req.setPassword("password123");

            mvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").exists())
                    .andExpect(jsonPath("$.refresh_token").exists());
        }

        @Test
        void rejectsInvalidCredentials() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setEmail("nonexistent@test.com");
            req.setPassword("wrong");

            mvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().is4xxClientError());
        }
    }
}
