package com.lifepath.filter;

import com.lifepath.util.SecurityUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final long TTL_SECONDS = 86400;

    private static class CachedResponse {
        final int status;
        final byte[] body;
        final String contentType;
        final Instant expiresAt;

        CachedResponse(int status, byte[] body, String contentType, Instant expiresAt) {
            this.status = status;
            this.body = body;
            this.contentType = contentType;
            this.expiresAt = expiresAt;
        }
    }

    private final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
        String key = request.getHeader(IDEMPOTENCY_HEADER);
        if (key == null || key.trim().isEmpty() || !isIdempotentPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<UUID> userId = SecurityUtil.currentUserIdOptional();
        if (!userId.isPresent()) {
            filterChain.doFilter(request, response);
            return;
        }

        String cacheKey = userId.get() + ":" + key.trim();
        CachedResponse cached = cache.get(cacheKey);
        if (cached != null && cached.expiresAt.isAfter(Instant.now())) {
            response.setStatus(cached.status);
            if (cached.contentType != null) response.setContentType(cached.contentType);
            if (cached.body != null && cached.body.length > 0) {
                response.getOutputStream().write(cached.body);
            }
            return;
        }

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, responseWrapper);
        } finally {
            byte[] body = responseWrapper.getContentAsByteArray();
            CachedResponse toCache = new CachedResponse(
                    responseWrapper.getStatus(),
                    body,
                    responseWrapper.getContentType(),
                    Instant.now().plusSeconds(TTL_SECONDS)
            );
            cache.put(cacheKey, toCache);
            responseWrapper.copyBodyToResponse();
        }
    }

    private boolean isIdempotentPath(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return false;
        String path = request.getRequestURI();
        if (path == null) return false;
        return path.matches(".*/quests/[^/]+/complete") ||
                path.matches(".*/quests/[^/]+/block") ||
                path.matches(".*/habits/[^/]+/complete");
    }
}
