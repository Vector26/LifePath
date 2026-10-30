package com.lifepath.controller;

import com.lifepath.dto.BlockTaskRequest;
import com.lifepath.dto.CurrentObjectiveResponse;
import com.lifepath.model.enums.ContextTag;
import com.lifepath.service.ObjectiveService;
import com.lifepath.service.TaskBlockingService;
import com.lifepath.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/objectives")
public class ObjectiveController {

    private final ObjectiveService objectiveService;
    private final TaskBlockingService taskBlockingService;

    public ObjectiveController(ObjectiveService objectiveService,
                               TaskBlockingService taskBlockingService) {
        this.objectiveService = objectiveService;
        this.taskBlockingService = taskBlockingService;
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentObjective(@RequestParam(required = false) ContextTag context) {
        UUID userId = SecurityUtil.currentUserId();
        return objectiveService.selectCurrentObjective(userId, context)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void blockTask(@Valid @RequestBody BlockTaskRequest request) {
        taskBlockingService.blockTask(SecurityUtil.currentUserId(), request);
    }
}
