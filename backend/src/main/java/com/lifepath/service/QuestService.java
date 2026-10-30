package com.lifepath.service;

import com.lifepath.dto.BlockQuestRequest;
import com.lifepath.dto.BlockQuestResponse;
import com.lifepath.dto.CompleteQuestResponse;
import com.lifepath.dto.CreateQuestRequest;
import com.lifepath.dto.QuestResponse;
import com.lifepath.dto.UpdateQuestRequest;
import com.lifepath.exception.ConflictException;
import com.lifepath.exception.UnprocessableEntityException;
import com.lifepath.model.BlockedQuest;
import com.lifepath.model.Quest;
import com.lifepath.model.User;
import com.lifepath.model.enums.QuestStatus;
import com.lifepath.model.enums.UnblockTrigger;
import com.lifepath.repository.BlockedQuestRepository;
import com.lifepath.repository.QuestRepository;
import com.lifepath.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class QuestService {

    private static final int XP_COMPLETE = 10;
    private static final int XP_BLOCK = 2;

    private final QuestRepository questRepository;
    private final BlockedQuestRepository blockedQuestRepository;
    private final UserRepository userRepository;
    private final PriorityAlgorithmService priorityAlgorithmService;

    public QuestService(QuestRepository questRepository,
                        BlockedQuestRepository blockedQuestRepository,
                        UserRepository userRepository,
                        PriorityAlgorithmService priorityAlgorithmService) {
        this.questRepository = questRepository;
        this.blockedQuestRepository = blockedQuestRepository;
        this.userRepository = userRepository;
        this.priorityAlgorithmService = priorityAlgorithmService;
    }

    public List<QuestResponse> listQuests(UUID userId, QuestStatus status, int limit) {
        List<Quest> quests = status != null
                ? questRepository.findByUserIdAndStatus(userId, status)
                : questRepository.findByUserId(userId, PageRequest.of(0, limit));
        return quests.stream().map(QuestResponse::from).collect(Collectors.toList());
    }

    public QuestResponse getQuest(UUID id, UUID userId) {
        Quest quest = questRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Quest not found"));
        return QuestResponse.from(quest);
    }

    @Transactional
    public QuestResponse createQuest(UUID userId, CreateQuestRequest req) {
        if (req.getParentId() != null && hasCycle(null, req.getParentId(), userId)) {
            throw new ConflictException("CIRCULAR_DEPENDENCY", "This change would create a circular dependency");
        }

        Quest quest = new Quest();
        quest.setUserId(userId);
        quest.setTitle(req.getTitle());
        quest.setNotes(req.getNotes());
        quest.setPriorityLevel(req.getPriorityLevel());
        quest.setParentId(req.getParentId());
        quest.setStatus(QuestStatus.pending);
        quest.setTimeOfDay(req.getTimeOfDay() != null ? req.getTimeOfDay() : com.lifepath.model.enums.TimeOfDay.any);
        quest.setContextTag(req.getContextTag());
        quest = questRepository.save(quest);
        return QuestResponse.from(quest);
    }

    @Transactional
    public QuestResponse updateQuest(UUID id, UUID userId, UpdateQuestRequest req) {
        Quest quest = questRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Quest not found"));

        UUID newParentId = req.getParentId() != null ? req.getParentId() : quest.getParentId();
        if (newParentId != null && hasCycle(id, newParentId, userId)) {
            throw new ConflictException("CIRCULAR_DEPENDENCY", "This change would create a circular dependency");
        }

        if (req.getTitle() != null) quest.setTitle(req.getTitle());
        if (req.getNotes() != null) quest.setNotes(req.getNotes());
        if (req.getPriorityLevel() != null) quest.setPriorityLevel(req.getPriorityLevel());
        if (req.getParentId() != null) quest.setParentId(req.getParentId());
        if (req.getTimeOfDay() != null) quest.setTimeOfDay(req.getTimeOfDay());
        if (req.getContextTag() != null) quest.setContextTag(req.getContextTag());

        quest = questRepository.save(quest);
        return QuestResponse.from(quest);
    }

    @Transactional
    public void deleteQuest(UUID id, UUID userId) {
        Quest quest = questRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Quest not found"));

        if (questRepository.countByParentId(id) > 0) {
            throw new ConflictException("QUEST_HAS_CHILDREN", "Cannot delete a quest that has dependent quests");
        }

        quest.setDeletedAt(Instant.now());
        questRepository.save(quest);
    }

    @Transactional
    public CompleteQuestResponse completeQuest(UUID id, UUID userId) {
        Quest quest = questRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Quest not found"));
        if (quest.getStatus() == QuestStatus.completed) {
            throw new UnprocessableEntityException("QUEST_ALREADY_COMPLETED", "Quest is already completed");
        }
        if (quest.getStatus() == QuestStatus.blocked) {
            throw new UnprocessableEntityException("QUEST_BLOCKED", "Cannot complete a blocked quest; unblock first");
        }

        quest.setStatus(QuestStatus.completed);
        quest.setCompletedAt(Instant.now());
        quest = questRepository.save(quest);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new java.util.NoSuchElementException("User not found"));
        user.setTotalXp(user.getTotalXp() + XP_COMPLETE);
        userRepository.save(user);

        return new CompleteQuestResponse(QuestResponse.from(quest), XP_COMPLETE);
    }

    @Transactional
    public BlockQuestResponse blockQuest(UUID id, UUID userId, BlockQuestRequest req) {
        Quest quest = questRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Quest not found"));
        if (quest.getStatus() == QuestStatus.completed) {
            throw new UnprocessableEntityException("QUEST_ALREADY_COMPLETED", "Cannot block a completed quest");
        }
        if (quest.getStatus() == QuestStatus.blocked) {
            throw new UnprocessableEntityException("QUEST_ALREADY_BLOCKED", "Quest is already blocked");
        }

        if (req.getUnblockTrigger() == UnblockTrigger.time && req.getUnblockAt() == null) {
            throw new UnprocessableEntityException("MISSING_UNBLOCK_AT", "unblock_at required when unblock_trigger is time");
        }
        if (req.getUnblockTrigger() == UnblockTrigger.location && (req.getUnblockLocation() == null || req.getUnblockLocation().trim().isEmpty())) {
            throw new UnprocessableEntityException("MISSING_UNBLOCK_LOCATION", "unblock_location required when unblock_trigger is location");
        }

        BlockedQuest blocked = new BlockedQuest();
        blocked.setQuestId(quest.getId());
        blocked.setReason(req.getReason());
        blocked.setUnblockTrigger(req.getUnblockTrigger());
        blocked.setUnblockAt(req.getUnblockAt());
        blocked.setUnblockLocation(req.getUnblockLocation());
        blockedQuestRepository.save(blocked);

        quest.setStatus(QuestStatus.blocked);
        quest = questRepository.save(quest);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new java.util.NoSuchElementException("User not found"));
        user.setTotalXp(user.getTotalXp() + XP_BLOCK);
        userRepository.save(user);

        Optional<Quest> next = priorityAlgorithmService.selectCurrentQuest(userId, null);
        QuestResponse nextResponse = next.map(QuestResponse::from).orElse(null);
        return new BlockQuestResponse(QuestResponse.from(quest), nextResponse, XP_BLOCK);
    }

    @Transactional
    public QuestResponse unblockQuest(UUID id, UUID userId) {
        Quest quest = questRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Quest not found"));
        if (quest.getStatus() != QuestStatus.blocked) {
            throw new UnprocessableEntityException("QUEST_NOT_BLOCKED", "Quest is not blocked");
        }

        BlockedQuest blocked = blockedQuestRepository.findByQuestIdAndUnblockedAtIsNull(id)
                .orElseThrow(() -> new UnprocessableEntityException("NO_ACTIVE_BLOCK", "No active block record for this quest"));

        blocked.setUnblockedAt(Instant.now());
        blockedQuestRepository.save(blocked);

        quest.setStatus(QuestStatus.pending);
        quest = questRepository.save(quest);
        return QuestResponse.from(quest);
    }

    private boolean hasCycle(UUID questId, UUID parentId, UUID userId) {
        UUID current = parentId;
        java.util.Set<UUID> visited = new java.util.HashSet<>();
        int depth = 0;
        while (current != null && depth < 1000) {
            if (current.equals(questId)) return true;
            if (visited.contains(current)) return true;
            visited.add(current);
            Quest parent = questRepository.findByIdAndUserId(current, userId).orElse(null);
            if (parent == null) break;
            current = parent.getParentId();
            depth++;
        }
        return false;
    }
}
