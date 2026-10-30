package com.lifepath.controller;

import com.lifepath.dto.CalendarEventResponse;
import com.lifepath.model.User;
import com.lifepath.repository.UserRepository;
import com.lifepath.service.CalendarService;
import com.lifepath.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping("/calendar")
public class CalendarController {

    @Value("${app.frontend-url:http://localhost:39173}")
    private String frontendUrl;

    private final CalendarService calendarService;
    private final UserRepository userRepository;

    public CalendarController(CalendarService calendarService,
                             UserRepository userRepository) {
        this.calendarService = calendarService;
        this.userRepository = userRepository;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        UUID userId = SecurityUtil.currentUserId();
        Map<String, Object> result = new HashMap<>();
        result.put("configured", calendarService.isConfigured());
        result.put("connected", calendarService.isConnected(userId));
        return result;
    }

    @GetMapping("/auth-url")
    public Map<String, String> getAuthUrl() {
        UUID userId = SecurityUtil.currentUserId();
        return Map.of("url", calendarService.buildAuthUrl(userId));
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> handleCallback(
            @RequestParam String code,
            @RequestParam String state) {
        UUID userId = UUID.fromString(state);
        calendarService.handleCallback(code, userId);
        return ResponseEntity.status(302).location(URI.create(frontendUrl + "/settings?calendar=connected")).build();
    }

    @PostMapping("/disconnect")
    public ResponseEntity<Void> disconnect() {
        calendarService.disconnect(SecurityUtil.currentUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/events/today")
    public List<CalendarEventResponse> getTodayEvents(@RequestParam(defaultValue = "false") boolean sync) {
        UUID userId = SecurityUtil.currentUserId();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found"));
        ZoneId zone = ZoneId.of(user.getTimezone());

        if (sync) {
            return calendarService.syncAndGetTodayEvents(userId, zone);
        }
        return calendarService.getTodayEvents(userId, zone);
    }

    @PostMapping("/events/{id}/complete")
    public CalendarEventResponse completeEvent(@PathVariable UUID id) {
        return calendarService.completeEvent(id, SecurityUtil.currentUserId());
    }
}
