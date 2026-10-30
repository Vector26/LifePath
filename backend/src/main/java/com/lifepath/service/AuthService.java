package com.lifepath.service;

import com.lifepath.dto.*;
import com.lifepath.model.CityState;
import com.lifepath.model.RefreshToken;
import com.lifepath.model.User;
import com.lifepath.repository.CityStateRepository;
import com.lifepath.repository.RefreshTokenRepository;
import com.lifepath.repository.UserRepository;
import com.lifepath.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CityStateRepository cityStateRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       CityStateRepository cityStateRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.cityStateRepository = cityStateRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new com.lifepath.exception.ConflictException("Email already exists");
        }

        User user = new User();
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setDisplayName(req.getDisplayName());
        user.setTimezone(req.getTimezone());
        user.setTotalXp(0);
        user = userRepository.save(user);

        CityState cityState = new CityState();
        cityState.setUserId(user.getId());
        cityState.setCoins(0);
        cityState.setCityName("My City");
        cityStateRepository.save(cityState);

        String accessToken = jwtService.createAccessToken(user.getId());
        String refreshToken = jwtService.createRefreshToken(user.getId());
        storeRefreshToken(user.getId(), refreshToken);

        UserResponse userResp = toUserResponse(user);
        return new RegisterResponse(userResp, accessToken, refreshToken, jwtService.getAccessTokenExpirySeconds());
    }

    public TokenResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new com.lifepath.exception.UnprocessableEntityException("UNAUTHORIZED", "Invalid credentials"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new com.lifepath.exception.UnprocessableEntityException("UNAUTHORIZED", "Invalid credentials");
        }

        String accessToken = jwtService.createAccessToken(user.getId());
        String refreshToken = jwtService.createRefreshToken(user.getId());
        storeRefreshToken(user.getId(), refreshToken);

        return new TokenResponse(accessToken, refreshToken, jwtService.getAccessTokenExpirySeconds());
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest req) {
        if (req.getRefreshToken() == null || req.getRefreshToken().trim().isEmpty()) {
            throw new com.lifepath.exception.UnprocessableEntityException("UNAUTHORIZED", "Refresh token required");
        }

        UUID userId = jwtService.parseRefreshToken(req.getRefreshToken());
        String tokenHash = sha256(req.getRefreshToken());

        List<RefreshToken> matches = refreshTokenRepository.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(
                tokenHash, Instant.now());
        if (matches.isEmpty()) {
            throw new com.lifepath.exception.UnprocessableEntityException("UNAUTHORIZED", "Invalid refresh token");
        }

        Instant now = Instant.now();
        matches.forEach(stored -> {
            stored.setRevokedAt(now);
            refreshTokenRepository.save(stored);
        });

        String newAccessToken = jwtService.createAccessToken(userId);
        String newRefreshToken = jwtService.createRefreshToken(userId);
        storeRefreshToken(userId, newRefreshToken);

        return new TokenResponse(newAccessToken, newRefreshToken, jwtService.getAccessTokenExpirySeconds());
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.findByUserId(userId).forEach(rt -> {
            rt.setRevokedAt(Instant.now());
            refreshTokenRepository.save(rt);
        });
    }

    private void storeRefreshToken(UUID userId, String token) {
        String tokenHash = sha256(token);
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        RefreshToken rt = new RefreshToken();
        rt.setUserId(userId);
        rt.setTokenHash(tokenHash);
        rt.setExpiresAt(expiresAt);
        refreshTokenRepository.save(rt);
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getTimezone(),
                user.getTotalXp(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
