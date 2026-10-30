package com.lifepath.controller;

import com.lifepath.dto.CreateHabitRequest;
import com.lifepath.dto.HabitCompletionResponse;
import com.lifepath.dto.HabitResponse;
import com.lifepath.dto.UpdateHabitRequest;
import com.lifepath.service.HabitService;
import com.lifepath.util.SecurityUtil;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/habits")
public class HabitController {

    private final HabitService habitService;

    public HabitController(HabitService habitService) {
        this.habitService = habitService;
    }

    @GetMapping
    public List<HabitResponse> listHabits() {
        return habitService.listHabits(SecurityUtil.currentUserId());
    }

    @GetMapping("/{id}")
    public HabitResponse getHabit(@PathVariable UUID id) {
        return habitService.getHabit(id, SecurityUtil.currentUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HabitResponse createHabit(@Valid @RequestBody CreateHabitRequest request) {
        return habitService.createHabit(SecurityUtil.currentUserId(), request);
    }

    @PatchMapping("/{id}")
    public HabitResponse updateHabit(@PathVariable UUID id, @Valid @RequestBody UpdateHabitRequest request) {
        return habitService.updateHabit(id, SecurityUtil.currentUserId(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHabit(@PathVariable UUID id) {
        habitService.deleteHabit(id, SecurityUtil.currentUserId());
    }

    @PostMapping("/{id}/complete")
    public HabitCompletionResponse completeHabit(@PathVariable UUID id) {
        return habitService.completeHabit(id, SecurityUtil.currentUserId());
    }
}
