package com.lifepath.controller;

import com.lifepath.dto.UpdateUserRequest;
import com.lifepath.dto.UserResponse;
import com.lifepath.dto.UserSettingsResponse;
import com.lifepath.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new org.springframework.security.access.AccessDeniedException("Not authenticated");
        }
        return UUID.fromString(auth.getPrincipal().toString());
    }

    @GetMapping("/me")
    public UserResponse getMe() {
        return userService.getCurrentUser(currentUserId());
    }

    @PatchMapping("/me")
    public UserResponse updateMe(@Valid @RequestBody UpdateUserRequest request) {
        return userService.updateUser(currentUserId(), request);
    }

    @GetMapping("/me/settings")
    public UserSettingsResponse getSettings() {
        return userService.getSettings(currentUserId());
    }
}
