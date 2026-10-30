package com.lifepath.service;

import com.lifepath.dto.UpdateUserRequest;
import com.lifepath.dto.UserResponse;
import com.lifepath.dto.UserSettingsResponse;
import com.lifepath.model.User;
import com.lifepath.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new java.util.NoSuchElementException("User not found"));
        return toUserResponse(user);
    }

    public UserResponse updateUser(UUID userId, UpdateUserRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new java.util.NoSuchElementException("User not found"));

        if (req.getDisplayName() != null) {
            user.setDisplayName(req.getDisplayName());
        }
        if (req.getTimezone() != null) {
            user.setTimezone(req.getTimezone());
        }
        user = userRepository.save(user);
        return toUserResponse(user);
    }

    public UserSettingsResponse getSettings(UUID userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new java.util.NoSuchElementException("User not found"));
        return new UserSettingsResponse();
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
