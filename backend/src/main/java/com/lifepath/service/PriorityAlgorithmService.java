package com.lifepath.service;

import com.lifepath.model.Quest;
import com.lifepath.model.User;
import com.lifepath.model.enums.ContextTag;
import com.lifepath.model.enums.PriorityLevel;
import com.lifepath.model.enums.QuestStatus;
import com.lifepath.repository.BlockedQuestRepository;
import com.lifepath.repository.QuestRepository;
import com.lifepath.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class PriorityAlgorithmService {

    private static final Map<PriorityLevel, Integer> BASE_SCORES = new HashMap<>();
    static {
        BASE_SCORES.put(PriorityLevel.P0, 1000);
        BASE_SCORES.put(PriorityLevel.P1, 400);
        BASE_SCORES.put(PriorityLevel.P2, 200);
        BASE_SCORES.put(PriorityLevel.P3, 100);
        BASE_SCORES.put(PriorityLevel.P4, 50);
    }

    private final QuestRepository questRepository;
    private final BlockedQuestRepository blockedQuestRepository;
    private final UserRepository userRepository;

    public PriorityAlgorithmService(QuestRepository questRepository,
                                    BlockedQuestRepository blockedQuestRepository,
                                    UserRepository userRepository) {
        this.questRepository = questRepository;
        this.blockedQuestRepository = blockedQuestRepository;
        this.userRepository = userRepository;
    }

    public Optional<Quest> selectCurrentQuest(UUID userId, ContextTag userContext) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new java.util.NoSuchElementException("User not found"));

        List<Quest> pending = questRepository.findByUserIdAndStatus(userId, QuestStatus.pending);
        Set<UUID> blockedQuestIds = pending.stream()
                .filter(q -> blockedQuestRepository.findByQuestIdAndUnblockedAtIsNull(q.getId()).isPresent())
                .map(Quest::getId)
                .collect(Collectors.toSet());

        List<Quest> eligible = pending.stream()
                .filter(q -> !blockedQuestIds.contains(q.getId()))
                .filter(q -> allAncestorsCompleted(q, userId))
                .collect(Collectors.toList());

        if (eligible.isEmpty()) return Optional.empty();

        ZoneId userZone = ZoneId.of(user.getTimezone());
        Instant now = Instant.now();

        Quest best = null;
        int bestScore = Integer.MIN_VALUE;

        for (Quest q : eligible) {
            int score = computeScore(q, user, userContext, userZone, now);
            if (score > bestScore) {
                bestScore = score;
                best = q;
            } else if (score == bestScore && best != null) {
                if (q.getCreatedAt().isBefore(best.getCreatedAt()) ||
                        (q.getCreatedAt().equals(best.getCreatedAt()) && q.getId().compareTo(best.getId()) < 0)) {
                    best = q;
                }
            }
        }

        return Optional.ofNullable(best);
    }

    public List<String> computeReasons(Quest q, User user, ContextTag userContext, ZoneId userZone, Instant now) {
        List<String> reasons = new ArrayList<>();
        int base = BASE_SCORES.getOrDefault(q.getPriorityLevel(), 50);
        if (base >= 400) reasons.add("high_priority");
        if (wasRecentlyUnblocked(q.getId(), now)) reasons.add("just_unblocked");
        if (q.getTimeOfDay() != null && matchesTimeOfDay(q.getTimeOfDay(), now, userZone)) {
            reasons.add("matches_" + q.getTimeOfDay().name());
        }
        if (userContext != null && q.getContextTag() == userContext) {
            reasons.add("matches_context");
        }
        long daysSinceCreation = ChronoUnit.DAYS.between(q.getCreatedAt(), now);
        if (daysSinceCreation <= 2) reasons.add("recently_added");
        return reasons;
    }

    private boolean allAncestorsCompleted(Quest q, UUID userId) {
        UUID current = q.getParentId();
        while (current != null) {
            Quest parent = questRepository.findByIdAndUserId(current, userId).orElse(null);
            if (parent == null) return false;
            if (parent.getStatus() != QuestStatus.completed) return false;
            current = parent.getParentId();
        }
        return true;
    }

    private int computeScore(Quest q, User user, ContextTag userContext, ZoneId userZone, Instant now) {
        int score = BASE_SCORES.getOrDefault(q.getPriorityLevel(), 50);

        long daysSinceCreation = ChronoUnit.DAYS.between(q.getCreatedAt(), now);
        score += Math.max(0, 50 - (int) (daysSinceCreation * 5));

        if (wasRecentlyUnblocked(q.getId(), now)) score += 200;

        if (q.getTimeOfDay() != null && matchesTimeOfDay(q.getTimeOfDay(), now, userZone)) {
            score += 20;
        }
        if (userContext != null && q.getContextTag() == userContext) {
            score += 25;
        }

        long daysSinceUpdate = ChronoUnit.DAYS.between(q.getUpdatedAt(), now);
        score -= Math.min(20, (int) (daysSinceUpdate * 2));

        return score;
    }

    private boolean wasRecentlyUnblocked(UUID questId, Instant now) {
        return blockedQuestRepository.findTopByQuestIdAndUnblockedAtIsNotNullOrderByUnblockedAtDesc(questId)
                .filter(bq -> Duration.between(bq.getUnblockedAt(), now).toHours() < 24)
                .isPresent();
    }

    private boolean matchesTimeOfDay(com.lifepath.model.enums.TimeOfDay tod, Instant now, ZoneId zone) {
        int hour = now.atZone(zone).getHour();
        switch (tod) {
            case morning: return hour >= 6 && hour < 12;
            case afternoon: return hour >= 12 && hour < 18;
            case evening: return hour >= 18 && hour < 22;
            case any: return true;
            default: return false;
        }
    }
}
