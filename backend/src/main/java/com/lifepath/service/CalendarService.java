package com.lifepath.service;

import com.lifepath.dto.CalendarEventResponse;
import com.lifepath.exception.CalendarApiDisabledException;
import com.lifepath.model.CalendarEvent;
import com.lifepath.model.GoogleCalendarToken;
import com.lifepath.repository.CalendarEventRepository;
import com.lifepath.repository.GoogleCalendarTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CalendarService {

    private static final Logger log = LoggerFactory.getLogger(CalendarService.class);

    @Value("${google.client-id:}")
    private String clientId;

    @Value("${google.client-secret:}")
    private String clientSecret;

    @Value("${google.redirect-uri:}")
    private String redirectUri;

    private final GoogleCalendarTokenRepository tokenRepository;
    private final CalendarEventRepository eventRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public CalendarService(GoogleCalendarTokenRepository tokenRepository,
                          CalendarEventRepository eventRepository) {
        this.tokenRepository = tokenRepository;
        this.eventRepository = eventRepository;
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
            && clientSecret != null && !clientSecret.isBlank();
    }

    public boolean isConnected(UUID userId) {
        return tokenRepository.findByUserId(userId).isPresent();
    }

    public String buildAuthUrl(UUID userId) {
        if (!isConfigured()) throw new IllegalStateException("Google Calendar not configured");
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new IllegalStateException("Google Calendar redirect_uri not configured");
        }
        String encodedRedirect = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        String encodedScope = URLEncoder.encode("https://www.googleapis.com/auth/calendar.readonly", StandardCharsets.UTF_8);
        return "https://accounts.google.com/o/oauth2/v2/auth"
            + "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
            + "&redirect_uri=" + encodedRedirect
            + "&response_type=code"
            + "&scope=" + encodedScope
            + "&access_type=offline"
            + "&prompt=consent"
            + "&state=" + userId;
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public void handleCallback(String code, UUID userId) {
        Map<String, String> body = new HashMap<>();
        body.put("code", code);
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("redirect_uri", redirectUri);
        body.put("grant_type", "authorization_code");

        ResponseEntity<Map> resp = restTemplate.postForEntity(
            "https://oauth2.googleapis.com/token", body, Map.class);

        Map<String, Object> data = resp.getBody();
        if (data == null) throw new RuntimeException("Empty token response");

        String accessToken = (String) data.get("access_token");
        String refreshToken = (String) data.get("refresh_token");
        int expiresIn = ((Number) data.get("expires_in")).intValue();

        GoogleCalendarToken token = tokenRepository.findByUserId(userId)
            .orElseGet(() -> {
                GoogleCalendarToken t = new GoogleCalendarToken();
                t.setUserId(userId);
                return t;
            });
        token.setAccessToken(accessToken);
        if (refreshToken != null) token.setRefreshToken(refreshToken);
        token.setTokenExpiry(Instant.now().plusSeconds(expiresIn));
        tokenRepository.save(token);
    }

    @Transactional
    public void disconnect(UUID userId) {
        tokenRepository.deleteByUserId(userId);
    }

    @Transactional
    public List<CalendarEventResponse> syncAndGetTodayEvents(UUID userId, ZoneId userZone) {
        log.info("syncAndGetTodayEvents: userId={}, userZone={}", userId, userZone);
        Optional<GoogleCalendarToken> optToken = tokenRepository.findByUserId(userId);
        if (optToken.isEmpty()) {
            log.info("syncAndGetTodayEvents: no token found for user {}", userId);
            return Collections.emptyList();
        }

        GoogleCalendarToken token = optToken.get();
        ensureTokenFresh(token);

        LocalDate today = LocalDate.now(userZone);
        Instant dayStart = today.atStartOfDay(userZone).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(userZone).toInstant();
        log.info("syncAndGetTodayEvents: today={}, dayStart={}, dayEnd={}", today, dayStart, dayEnd);

        fetchAndStoreEvents(token, userId, dayStart, dayEnd);

        List<CalendarEventResponse> events = eventRepository.findByUserIdAndStartTimeBetween(userId, dayStart, dayEnd)
            .stream()
            .map(CalendarEventResponse::from)
            .collect(Collectors.toList());
        log.info("syncAndGetTodayEvents: returning {} events", events.size());
        return events;
    }

    /**
     * Syncs today's events from Google Calendar if the user has a token.
     * No-op if not connected; swallows errors so callers can fall back to DB data.
     */
    public void syncTodayIfConnected(UUID userId, ZoneId userZone) {
        if (!isConnected(userId)) return;
        try {
            syncAndGetTodayEvents(userId, userZone);
        } catch (Exception e) {
            log.warn("syncTodayIfConnected failed for user {}: {}", userId, e.getMessage());
        }
    }

    public List<CalendarEventResponse> getTodayEvents(UUID userId, ZoneId userZone) {
        LocalDate today = LocalDate.now(userZone);
        Instant dayStart = today.atStartOfDay(userZone).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(userZone).toInstant();

        return eventRepository.findByUserIdAndStartTimeBetween(userId, dayStart, dayEnd)
            .stream()
            .map(CalendarEventResponse::from)
            .collect(Collectors.toList());
    }

    public List<CalendarEvent> getIncompleteTodayEvents(UUID userId, ZoneId userZone) {
        LocalDate today = LocalDate.now(userZone);
        Instant dayStart = today.atStartOfDay(userZone).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(userZone).toInstant();

        return eventRepository.findByUserIdAndCompletedFalseAndStartTimeBetween(userId, dayStart, dayEnd);
    }

    @Transactional
    public CalendarEventResponse completeEvent(UUID eventId, UUID userId) {
        CalendarEvent event = eventRepository.findByIdAndUserId(eventId, userId)
            .orElseThrow(() -> new NoSuchElementException("Calendar event not found"));
        event.setCompleted(true);
        eventRepository.save(event);
        return CalendarEventResponse.from(event);
    }

    @SuppressWarnings("unchecked")
    private void ensureTokenFresh(GoogleCalendarToken token) {
        if (token.getTokenExpiry().isAfter(Instant.now().plusSeconds(60))) return;

        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException(
                "Google Calendar OAuth not configured. Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET.");
        }
        String refreshToken = token.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalStateException(
                "Google Calendar refresh token is missing. Reconnect your calendar in Settings.");
        }

        Map<String, String> body = new HashMap<>();
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("refresh_token", refreshToken);
        body.put("grant_type", "refresh_token");

        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                "https://oauth2.googleapis.com/token", body, Map.class);
            Map<String, Object> data = resp.getBody();
            if (data == null) return;

            token.setAccessToken((String) data.get("access_token"));
            int expiresIn = ((Number) data.get("expires_in")).intValue();
            token.setTokenExpiry(Instant.now().plusSeconds(expiresIn));
            tokenRepository.save(token);
        } catch (HttpStatusCodeException e) {
            String errBody = e.getResponseBodyAsString();
            if (e.getStatusCode().value() == 400 && errBody != null && errBody.contains("client ID")) {
                throw new IllegalStateException(
                    "Google Calendar OAuth not configured. Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET.");
            }
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private void fetchAndStoreEvents(GoogleCalendarToken token, UUID userId,
                                     Instant timeMin, Instant timeMax) {
        String url = "https://www.googleapis.com/calendar/v3/calendars/primary/events"
            + "?timeMin=" + DateTimeFormatter.ISO_INSTANT.format(timeMin)
            + "&timeMax=" + DateTimeFormatter.ISO_INSTANT.format(timeMax)
            + "&singleEvents=true"
            + "&orderBy=startTime";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.getAccessToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        log.info("fetchAndStoreEvents: calling Google Calendar API for user {} from {} to {}", userId, timeMin, timeMax);
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> data = resp.getBody();
            if (data == null) {
                log.warn("fetchAndStoreEvents: null response body from Google");
                return;
            }

            List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
            if (items == null) {
                log.info("fetchAndStoreEvents: no items in response");
                return;
            }

            log.info("fetchAndStoreEvents: Google returned {} items", items.size());
            Set<String> syncedExternalIds = new HashSet<>();
            for (Map<String, Object> item : items) {
                String externalId = (String) item.get("id");
                if (externalId != null && !externalId.isBlank()) {
                    syncedExternalIds.add(externalId);
                }
                String title = (String) item.get("summary");
                if (title == null) title = "(No title)";

                Instant start = parseGoogleTime((Map<String, Object>) item.get("start"));
                Instant end = parseGoogleTime((Map<String, Object>) item.get("end"));
                if (start == null || end == null) {
                    log.warn("fetchAndStoreEvents: skipping event '{}' — could not parse start/end", title);
                    continue;
                }

                log.info("fetchAndStoreEvents: saving event '{}' start={} end={}", title, start, end);
                CalendarEvent event = eventRepository.findByUserIdAndExternalId(userId, externalId)
                    .orElseGet(() -> {
                        CalendarEvent e = new CalendarEvent();
                        e.setUserId(userId);
                        e.setExternalId(externalId);
                        return e;
                    });
                event.setTitle(title);
                event.setStartTime(start);
                event.setEndTime(end);
                event.setSyncedAt(Instant.now());
                eventRepository.save(event);
            }

            if (syncedExternalIds.isEmpty()) {
                eventRepository.deleteByUserIdAndStartTimeBetween(userId, timeMin, timeMax);
                log.info("fetchAndStoreEvents: removed all local events in range for user {}", userId);
            } else {
                eventRepository.deleteStaleInRange(userId, timeMin, timeMax, new ArrayList<>(syncedExternalIds));
                log.info("fetchAndStoreEvents: removed stale local events not returned by Google for user {}", userId);
            }
        } catch (CalendarApiDisabledException e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof HttpStatusCodeException hce && hce.getStatusCode().value() == 403) {
                String body = hce.getResponseBodyAsString();
                if (body != null && (body.contains("accessNotConfigured") || body.contains("SERVICE_DISABLED"))) {
                    String activationUrl = "https://console.developers.google.com/apis/api/calendar-json.googleapis.com/overview";
                    throw new CalendarApiDisabledException(
                        "Google Calendar API is not enabled for this project. Enable it in Google Cloud Console.",
                        activationUrl);
                }
            }
            log.error("fetchAndStoreEvents: Google Calendar API call failed for user {}: {}", userId, e.getMessage(), e);
        }
    }

    private Instant parseGoogleTime(Map<String, Object> time) {
        if (time == null) return null;
        String dateTime = (String) time.get("dateTime");
        if (dateTime != null) {
            try {
                return OffsetDateTime.parse(dateTime).toInstant();
            } catch (Exception e) {
                log.warn("parseGoogleTime: failed to parse dateTime '{}': {}", dateTime, e.getMessage());
                return null;
            }
        }
        String date = (String) time.get("date");
        if (date != null) return LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant();
        return null;
    }
}
