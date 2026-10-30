package com.lifepath.controller;

import com.lifepath.service.CalendarService;
import com.lifepath.repository.UserRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.ZoneId;
import java.util.UUID;

/**
 * Manual trigger to reproduce calendar INTERNAL_ERROR. Run with:
 * mvn test -Dtest=CalendarDebugTriggerTest -Dspring.profiles.active=postgres
 * Requires postgres running. Set DEBUG_USER_ID env to the user UUID with calendar connected.
 */
@SpringBootTest
@ActiveProfiles("postgres")
@Disabled("Manual debug only - enable to run")
class CalendarDebugTriggerTest {

    @Autowired CalendarService calendarService;
    @Autowired UserRepository userRepository;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry reg) {
        reg.add("spring.flyway.clean-disabled", () -> "true");
    }

    @Test
    void triggerGetTodayEvents() {
        String userIdStr = System.getenv("DEBUG_USER_ID");
        if (userIdStr == null || userIdStr.isBlank()) {
            throw new IllegalStateException("Set DEBUG_USER_ID to a user UUID with calendar connected");
        }
        UUID userId = UUID.fromString(userIdStr);
        var user = userRepository.findById(userId).orElseThrow();
        var events = calendarService.syncAndGetTodayEvents(userId, ZoneId.of(user.getTimezone()));
        System.out.println("Events: " + events.size());
    }
}
