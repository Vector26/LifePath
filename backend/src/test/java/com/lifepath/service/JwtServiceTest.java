package com.lifepath.service;

import com.lifepath.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    JwtService jwtService;
    UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("test-access-secret-min-256-bits-for-hs256-signing",
                "test-refresh-secret-min-256-bits-for-hs256-signing");
    }

    @Nested
    @DisplayName("Token creation")
    class Creation {

        @Test
        void createAccessToken() {
            String token = jwtService.createAccessToken(userId);
            assertThat(token).isNotBlank();
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        void createRefreshToken() {
            String token = jwtService.createRefreshToken(userId);
            assertThat(token).isNotBlank();
            assertThat(token.split("\\.")).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Token parsing")
    class Parsing {

        @Test
        void parseValidAccessToken() {
            String token = jwtService.createAccessToken(userId);
            UUID parsed = jwtService.parseAccessToken(token);
            assertThat(parsed).isEqualTo(userId);
        }

        @Test
        void parseValidRefreshToken() {
            String token = jwtService.createRefreshToken(userId);
            UUID parsed = jwtService.parseRefreshToken(token);
            assertThat(parsed).isEqualTo(userId);
        }

        @Test
        void parseInvalidTokenThrows() {
            assertThatThrownBy(() -> jwtService.parseAccessToken("invalid.token.here"))
                    .isInstanceOf(JwtService.JwtAuthenticationException.class);
        }

        @Test
        void parseWrongTokenTypeThrows() {
            String refreshToken = jwtService.createRefreshToken(userId);
            assertThatThrownBy(() -> jwtService.parseAccessToken(refreshToken))
                    .isInstanceOf(JwtService.JwtAuthenticationException.class);
        }
    }

    @Nested
    @DisplayName("Expiry")
    class Expiry {

        @Test
        void accessTokenExpiryConfigured() {
            assertThat(jwtService.getAccessTokenExpirySeconds()).isEqualTo(900);
        }
    }
}
