package com.lifepath.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifepath.dto.RegisterRequest;
import com.lifepath.model.CalendarEvent;
import com.lifepath.model.GoogleCalendarToken;
import com.lifepath.repository.CalendarEventRepository;
import com.lifepath.repository.GoogleCalendarTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test that verifies calendar events actually appear in the objective queue.
 * Inserts a real calendar event into the DB and asserts GET /objectives/current returns it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ObjectiveCalendarIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired GoogleCalendarTokenRepository tokenRepository;
    @Autowired CalendarEventRepository eventRepository;

    String accessToken;
    UUID userId;

    @BeforeEach
    void setUp() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("caluser-" + UUID.randomUUID() + "@test.com");
        reg.setPassword("password123");
        reg.setTimezone("UTC");
        var regResult = mvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();
        var body = objectMapper.readTree(regResult.getResponse().getContentAsString());
        accessToken = body.get("access_token").asText();
        userId = UUID.fromString(body.get("user").get("id").asText());
    }

    @Test
    @DisplayName("GET /objectives/current returns calendar_event when user has incomplete event today")
    void returnsCalendarEventInObjectiveQueue() throws Exception {
        // Simulate "connected" user by inserting a token (so syncTodayIfConnected doesn't fail)
        GoogleCalendarToken token = new GoogleCalendarToken();
        token.setUserId(userId);
        token.setAccessToken("fake-access-token");
        token.setRefreshToken("fake-refresh-token");
        token.setTokenExpiry(Instant.now().plus(1, ChronoUnit.HOURS));
        tokenRepository.save(token);

        // Insert an incomplete calendar event for today
        ZoneId zone = ZoneId.of("UTC");
        LocalDate today = LocalDate.now(zone);
        Instant dayStart = today.atStartOfDay(zone).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant();
        Instant eventStart = dayStart.plus(2, ChronoUnit.HOURS);
        Instant eventEnd = eventStart.plus(1, ChronoUnit.HOURS);

        CalendarEvent event = new CalendarEvent();
        event.setUserId(userId);
        event.setExternalId("test-ext-" + UUID.randomUUID());
        event.setTitle("Team standup");
        event.setStartTime(eventStart);
        event.setEndTime(eventEnd);
        event.setCompleted(false);
        event.setSyncedAt(Instant.now());
        eventRepository.save(event);

        mvc.perform(get("/objectives/current")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("calendar_event"))
                .andExpect(jsonPath("$.payload.id").exists())
                .andExpect(jsonPath("$.payload.title").value("Team standup"))
                .andExpect(jsonPath("$.payload.completed").value(false));
    }

    @Test
    @DisplayName("GET /objectives/current does not return completed calendar events")
    void excludesCompletedCalendarEvents() throws Exception {
        GoogleCalendarToken token = new GoogleCalendarToken();
        token.setUserId(userId);
        token.setAccessToken("fake-access-token");
        token.setRefreshToken("fake-refresh-token");
        token.setTokenExpiry(Instant.now().plus(1, ChronoUnit.HOURS));
        tokenRepository.save(token);

        ZoneId zone = ZoneId.of("UTC");
        LocalDate today = LocalDate.now(zone);
        Instant dayStart = today.atStartOfDay(zone).toInstant();
        Instant eventStart = dayStart.plus(2, ChronoUnit.HOURS);
        Instant eventEnd = eventStart.plus(1, ChronoUnit.HOURS);

        CalendarEvent event = new CalendarEvent();
        event.setUserId(userId);
        event.setExternalId("test-ext-completed-" + UUID.randomUUID());
        event.setTitle("Done meeting");
        event.setStartTime(eventStart);
        event.setEndTime(eventEnd);
        event.setCompleted(true);
        event.setSyncedAt(Instant.now());
        eventRepository.save(event);

        // With only a completed event, we may get 204 (no objective) or a quest/habit if any exist
        var result = mvc.perform(get("/objectives/current")
                .header("Authorization", "Bearer " + accessToken))
                .andReturn();

        // Should NOT return this event - either 204 or a different objective type
        if (result.getResponse().getStatus() == 200) {
            String content = result.getResponse().getContentAsString();
            if (content.contains("calendar_event") && content.contains("Done meeting")) {
                throw new AssertionError("Completed calendar event should not appear in objective queue");
            }
        }
    }
}
