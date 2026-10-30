package com.lifepath.service;

import com.lifepath.dto.*;
import com.lifepath.dto.CurrentObjectiveResponse.ObjectiveType;
import com.lifepath.model.*;
import com.lifepath.model.enums.ContextTag;
import com.lifepath.model.enums.QuestStatus;
import com.lifepath.model.enums.TaskType;
import com.lifepath.model.enums.UnblockTrigger;
import com.lifepath.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
public class ObjectiveService {

    private static final Duration MANUAL_BLOCK_RECYCLE_DELAY = Duration.ofSeconds(5);

    private final PriorityAlgorithmService priorityAlgorithmService;
    private final HabitRepository habitRepository;
    private final HabitCompletionRepository habitCompletionRepository;
    private final CalendarService calendarService;
    private final UserRepository userRepository;
    private final TaskBlockingService taskBlockingService;
    private final BlockedQuestRepository blockedQuestRepository;
    private final BlockedTaskRepository blockedTaskRepository;
    private final QuestRepository questRepository;

    public ObjectiveService(PriorityAlgorithmService priorityAlgorithmService,
                           HabitRepository habitRepository,
                           HabitCompletionRepository habitCompletionRepository,
                           CalendarService calendarService,
                           UserRepository userRepository,
                           TaskBlockingService taskBlockingService,
                           BlockedQuestRepository blockedQuestRepository,
                           BlockedTaskRepository blockedTaskRepository,
                           QuestRepository questRepository) {
        this.priorityAlgorithmService = priorityAlgorithmService;
        this.habitRepository = habitRepository;
        this.habitCompletionRepository = habitCompletionRepository;
        this.calendarService = calendarService;
        this.userRepository = userRepository;
        this.taskBlockingService = taskBlockingService;
        this.blockedQuestRepository = blockedQuestRepository;
        this.blockedTaskRepository = blockedTaskRepository;
        this.questRepository = questRepository;
    }

    @Transactional
    public Optional<CurrentObjectiveResponse> selectCurrentObjective(UUID userId, ContextTag context) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found"));
        ZoneId zone = ZoneId.of(user.getTimezone());
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(zone);
        int hour = now.atZone(zone).getHour();
        Set<UUID> blockedHabitIds = taskBlockingService.getActiveBlockedTaskIds(userId, TaskType.habit, now);
        Set<UUID> blockedEventIds = taskBlockingService.getActiveBlockedTaskIds(userId, TaskType.calendar_event, now);

        List<ScoredItem> candidates = new ArrayList<>();

        // 1. Quests (via existing algorithm)
        priorityAlgorithmService.selectCurrentQuest(userId, context).ifPresent(quest -> {
            List<String> reasons = priorityAlgorithmService.computeReasons(quest, user, context, zone, now);
            int score = 500; // quests get a strong base
            if (quest.getPriorityLevel() != null) {
                switch (quest.getPriorityLevel()) {
                    case P0: score = 1000; break;
                    case P1: score = 700; break;
                    case P2: score = 500; break;
                    case P3: score = 300; break;
                    case P4: score = 200; break;
                }
            }
            candidates.add(new ScoredItem(
                ObjectiveType.quest,
                new CurrentQuestResponse(QuestResponse.from(quest), reasons),
                score,
                reasons
            ));
        });

        // 2. Habits (incomplete today, streak-tracked preferred)
        List<Habit> habits = habitRepository.findByUserId(userId);
        for (Habit habit : habits) {
            if (habit.getDeletedAt() != null) continue;
            if (blockedHabitIds.contains(habit.getId())) continue;
            boolean completedToday = habitCompletionRepository
                .findByHabitIdAndCompletionDate(habit.getId(), today).isPresent();
            if (completedToday) continue;

            int score = 150;
            if (habit.isStreakTracked()) score += 100;
            // Morning habits get priority in morning, etc
            if (hour < 12) score += 30;
            else if (hour < 18) score += 10;

            List<String> reasons = new ArrayList<>();
            reasons.add("habit_due_today");
            if (habit.isStreakTracked()) reasons.add("streak_tracked");

            candidates.add(new ScoredItem(
                ObjectiveType.habit,
                HabitResponse.from(habit),
                score,
                reasons
            ));
        }

        // 3. Calendar events (incomplete, happening soon gets priority)
        try {
            calendarService.syncTodayIfConnected(userId, zone);
        } catch (Exception ignored) {
            /* Fall back to stale/empty DB data on sync failure */
        }
        List<CalendarEvent> events = calendarService.getIncompleteTodayEvents(userId, zone);
        for (CalendarEvent event : events) {
            if (blockedEventIds.contains(event.getId())) continue;
            int score = 100;
            long minutesUntilStart = Duration.between(now, event.getStartTime()).toMinutes();
            // Upcoming events within 2 hours get highest calendar priority
            if (minutesUntilStart <= 0 && Duration.between(now, event.getEndTime()).toMinutes() > 0) {
                score = 900; // happening now
            } else if (minutesUntilStart > 0 && minutesUntilStart <= 30) {
                score = 800; // starting very soon
            } else if (minutesUntilStart > 0 && minutesUntilStart <= 120) {
                score = 600; // starting within 2 hours
            }

            List<String> reasons = new ArrayList<>();
            if (minutesUntilStart <= 0) reasons.add("happening_now");
            else if (minutesUntilStart <= 30) reasons.add("starting_soon");
            else reasons.add("scheduled_today");

            candidates.add(new ScoredItem(
                ObjectiveType.calendar_event,
                CalendarEventResponse.from(event),
                score,
                reasons
            ));
        }

        if (candidates.isEmpty()) {
            return recycleOldestManualBlock(userId, context, now, zone, today, hour);
        }

        candidates.sort((a, b) -> Integer.compare(b.score, a.score));
        ScoredItem best = candidates.get(0);

        return Optional.of(new CurrentObjectiveResponse(best.type, best.payload, best.reasons));
    }

    private Optional<CurrentObjectiveResponse> recycleOldestManualBlock(
            UUID userId, ContextTag context, Instant now, ZoneId zone, LocalDate today, int hour) {

        Instant oldestQuestBlockTime = null;
        BlockedQuest oldestQuestBlock = null;
        Quest oldestQuest = null;

        List<BlockedQuest> questBlocks = blockedQuestRepository
                .findManualBlocksForUser(userId, UnblockTrigger.manual, PageRequest.of(0, 1));
        if (!questBlocks.isEmpty()) {
            BlockedQuest bq = questBlocks.get(0);
            Optional<Quest> q = questRepository.findByIdAndUserId(bq.getQuestId(), userId);
            if (q.isPresent() && q.get().getStatus() == QuestStatus.blocked) {
                oldestQuestBlock = bq;
                oldestQuest = q.get();
                oldestQuestBlockTime = bq.getCreatedAt();
            }
        }

        Instant oldestTaskBlockTime = null;
        BlockedTask oldestTaskBlock = null;

        Optional<BlockedTask> taskBlock = blockedTaskRepository
                .findTopByUserIdAndUnblockTriggerAndUnblockedAtIsNullOrderByCreatedAtAsc(userId, UnblockTrigger.manual);
        if (taskBlock.isPresent()) {
            oldestTaskBlock = taskBlock.get();
            oldestTaskBlockTime = oldestTaskBlock.getCreatedAt();
        }

        if (oldestQuestBlock == null && oldestTaskBlock == null) {
            return Optional.empty();
        }

        boolean unblockQuest = oldestQuestBlockTime != null
                && (oldestTaskBlockTime == null || !oldestQuestBlockTime.isAfter(oldestTaskBlockTime));
        Instant selectedBlockTime = unblockQuest ? oldestQuestBlockTime : oldestTaskBlockTime;

        // If user just skipped/blocked this task, do not recycle it immediately.
        if (selectedBlockTime != null && selectedBlockTime.isAfter(now.minus(MANUAL_BLOCK_RECYCLE_DELAY))) {
            return Optional.empty();
        }

        if (unblockQuest) {
            oldestQuestBlock.setUnblockedAt(now);
            blockedQuestRepository.save(oldestQuestBlock);
            oldestQuest.setStatus(QuestStatus.pending);
            questRepository.save(oldestQuest);
        } else {
            oldestTaskBlock.setUnblockedAt(now);
            blockedTaskRepository.save(oldestTaskBlock);
        }

        return selectCurrentObjective(userId, context);
    }

    private static class ScoredItem {
        final ObjectiveType type;
        final Object payload;
        final int score;
        final List<String> reasons;

        ScoredItem(ObjectiveType type, Object payload, int score, List<String> reasons) {
            this.type = type;
            this.payload = payload;
            this.score = score;
            this.reasons = reasons;
        }
    }
}
