package com.lifepath.controller;

import com.lifepath.dto.BlockQuestRequest;
import com.lifepath.dto.BlockQuestResponse;
import com.lifepath.dto.CompleteQuestResponse;
import com.lifepath.dto.CreateQuestRequest;
import com.lifepath.dto.CurrentQuestResponse;
import com.lifepath.dto.QuestResponse;
import com.lifepath.dto.UpdateQuestRequest;
import com.lifepath.model.User;
import com.lifepath.model.enums.ContextTag;
import com.lifepath.model.enums.QuestStatus;
import com.lifepath.repository.UserRepository;
import com.lifepath.service.PriorityAlgorithmService;
import com.lifepath.service.QuestService;
import com.lifepath.util.SecurityUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/quests")
public class QuestController {

    private final QuestService questService;
    private final PriorityAlgorithmService priorityAlgorithmService;
    private final UserRepository userRepository;

    public QuestController(QuestService questService,
                           PriorityAlgorithmService priorityAlgorithmService,
                           UserRepository userRepository) {
        this.questService = questService;
        this.priorityAlgorithmService = priorityAlgorithmService;
        this.userRepository = userRepository;
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentQuest(@RequestParam(required = false) ContextTag context) {
        UUID userId = SecurityUtil.currentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new java.util.NoSuchElementException("User not found"));
        return priorityAlgorithmService.selectCurrentQuest(userId, context)
                .map(q -> {
                    ZoneId zone = ZoneId.of(user.getTimezone());
                    Instant now = Instant.now();
                    List<String> reasons = priorityAlgorithmService.computeReasons(q, user, context, zone, now);
                    return ResponseEntity.ok(new CurrentQuestResponse(QuestResponse.from(q), reasons));
                })
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping
    public List<QuestResponse> listQuests(
            @RequestParam(required = false) QuestStatus status,
            @RequestParam(defaultValue = "50") int limit) {
        UUID userId = SecurityUtil.currentUserId();
        return questService.listQuests(userId, status, Math.min(limit, 100));
    }

    @GetMapping("/{id}")
    public QuestResponse getQuest(@PathVariable UUID id) {
        return questService.getQuest(id, SecurityUtil.currentUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestResponse createQuest(@Valid @RequestBody CreateQuestRequest request) {
        return questService.createQuest(SecurityUtil.currentUserId(), request);
    }

    @PatchMapping("/{id}")
    public QuestResponse updateQuest(@PathVariable UUID id, @Valid @RequestBody UpdateQuestRequest request) {
        return questService.updateQuest(id, SecurityUtil.currentUserId(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuest(@PathVariable UUID id) {
        questService.deleteQuest(id, SecurityUtil.currentUserId());
    }

    @PostMapping("/{id}/complete")
    public CompleteQuestResponse completeQuest(@PathVariable UUID id) {
        return questService.completeQuest(id, SecurityUtil.currentUserId());
    }

    @PostMapping("/{id}/block")
    public BlockQuestResponse blockQuest(@PathVariable UUID id, @Valid @RequestBody BlockQuestRequest request) {
        return questService.blockQuest(id, SecurityUtil.currentUserId(), request);
    }

    @PostMapping("/{id}/unblock")
    public QuestResponse unblockQuest(@PathVariable UUID id) {
        return questService.unblockQuest(id, SecurityUtil.currentUserId());
    }
}
