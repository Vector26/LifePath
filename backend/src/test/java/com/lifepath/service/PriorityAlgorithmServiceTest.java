package com.lifepath.service;

import com.lifepath.model.Quest;
import com.lifepath.model.User;
import com.lifepath.model.enums.ContextTag;
import com.lifepath.model.enums.PriorityLevel;
import com.lifepath.model.enums.QuestStatus;
import com.lifepath.repository.BlockedQuestRepository;
import com.lifepath.repository.QuestRepository;
import com.lifepath.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

import static com.lifepath.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class PriorityAlgorithmServiceTest {

    @Mock QuestRepository questRepository;
    @Mock BlockedQuestRepository blockedQuestRepository;
    @Mock UserRepository userRepository;

    PriorityAlgorithmService service;

    UUID userId = UUID.randomUUID();
    ZoneId utc = ZoneId.of("UTC");

    @BeforeEach
    void setUp() {
        service = new PriorityAlgorithmService(questRepository, blockedQuestRepository, userRepository);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, "UTC")));
    }

    @Nested
    @DisplayName("Base priority scoring")
    class BasePriority {

        @Test
        void p0BeatsAll() {
            Quest p0 = quest(UUID.randomUUID(), userId, PriorityLevel.P0, null, QuestStatus.pending);
            Quest p1 = quest(UUID.randomUUID(), userId, PriorityLevel.P1, null, QuestStatus.pending);
            when(questRepository.findByUserIdAndStatus(userId, QuestStatus.pending))
                    .thenReturn(List.of(p0, p1));
            when(blockedQuestRepository.findByQuestIdAndUnblockedAtIsNull(any())).thenReturn(Optional.empty());
            when(blockedQuestRepository.findTopByQuestIdAndUnblockedAtIsNotNullOrderByUnblockedAtDesc(any())).thenReturn(Optional.empty());

            assertThat(service.selectCurrentQuest(userId, null)).contains(p0);
        }

        @Test
        void p1BeatsP2() {
            Quest p1 = quest(UUID.randomUUID(), userId, PriorityLevel.P1, null, QuestStatus.pending);
            Quest p2 = quest(UUID.randomUUID(), userId, PriorityLevel.P2, null, QuestStatus.pending);
            when(questRepository.findByUserIdAndStatus(userId, QuestStatus.pending))
                    .thenReturn(List.of(p1, p2));
            when(blockedQuestRepository.findByQuestIdAndUnblockedAtIsNull(any())).thenReturn(Optional.empty());
            when(blockedQuestRepository.findTopByQuestIdAndUnblockedAtIsNotNullOrderByUnblockedAtDesc(any())).thenReturn(Optional.empty());

            assertThat(service.selectCurrentQuest(userId, null)).contains(p1);
        }

        @Test
        void p4HasLowestBaseScore() {
            Quest p4 = quest(UUID.randomUUID(), userId, PriorityLevel.P4, null, QuestStatus.pending);
            when(questRepository.findByUserIdAndStatus(userId, QuestStatus.pending)).thenReturn(List.of(p4));
            when(blockedQuestRepository.findByQuestIdAndUnblockedAtIsNull(any())).thenReturn(Optional.empty());
            when(blockedQuestRepository.findTopByQuestIdAndUnblockedAtIsNotNullOrderByUnblockedAtDesc(any())).thenReturn(Optional.empty());

            assertThat(service.selectCurrentQuest(userId, null)).contains(p4);
        }
    }

    @Nested
    @DisplayName("Blocked quests excluded")
    class BlockedExclusion {

        @Test
        void blockedQuestNotEligible() {
            Quest pending = quest(UUID.randomUUID(), userId, PriorityLevel.P0, null, QuestStatus.pending);
            when(questRepository.findByUserIdAndStatus(userId, QuestStatus.pending)).thenReturn(List.of(pending));
            when(blockedQuestRepository.findByQuestIdAndUnblockedAtIsNull(pending.getId()))
                    .thenReturn(Optional.of(blockedQuest(pending.getId(), null)));
            when(blockedQuestRepository.findTopByQuestIdAndUnblockedAtIsNotNullOrderByUnblockedAtDesc(any())).thenReturn(Optional.empty());

            assertThat(service.selectCurrentQuest(userId, null)).isEmpty();
        }

        @Test
        void unblockedQuestEligible() {
            Quest pending = quest(UUID.randomUUID(), userId, PriorityLevel.P0, null, QuestStatus.pending);
            when(questRepository.findByUserIdAndStatus(userId, QuestStatus.pending)).thenReturn(List.of(pending));
            when(blockedQuestRepository.findByQuestIdAndUnblockedAtIsNull(any())).thenReturn(Optional.empty());
            when(blockedQuestRepository.findTopByQuestIdAndUnblockedAtIsNotNullOrderByUnblockedAtDesc(any())).thenReturn(Optional.empty());

            assertThat(service.selectCurrentQuest(userId, null)).contains(pending);
        }
    }

    @Nested
    @DisplayName("Dependency / parent chain")
    class ParentChain {

        @Test
        void childIneligibleWhenParentNotCompleted() {
            UUID parentId = UUID.randomUUID();
            Quest parent = quest(parentId, userId, PriorityLevel.P0, null, QuestStatus.pending);
            Quest child = quest(UUID.randomUUID(), userId, PriorityLevel.P1, parentId, QuestStatus.pending);
            when(questRepository.findByUserIdAndStatus(userId, QuestStatus.pending))
                    .thenReturn(List.of(parent, child));
            when(blockedQuestRepository.findByQuestIdAndUnblockedAtIsNull(any())).thenReturn(Optional.empty());
            when(blockedQuestRepository.findTopByQuestIdAndUnblockedAtIsNotNullOrderByUnblockedAtDesc(any())).thenReturn(Optional.empty());

            assertThat(service.selectCurrentQuest(userId, null)).contains(parent);
        }

        @Test
        void childEligibleWhenParentCompleted() {
            UUID parentId = UUID.randomUUID();
            Quest parent = quest(parentId, userId, PriorityLevel.P0, null, QuestStatus.completed);
            Quest child = quest(UUID.randomUUID(), userId, PriorityLevel.P1, parentId, QuestStatus.pending);
            when(questRepository.findByUserIdAndStatus(userId, QuestStatus.pending)).thenReturn(List.of(child));
            when(questRepository.findByIdAndUserId(parentId, userId)).thenReturn(Optional.of(parent));
            when(blockedQuestRepository.findByQuestIdAndUnblockedAtIsNull(any())).thenReturn(Optional.empty());
            when(blockedQuestRepository.findTopByQuestIdAndUnblockedAtIsNotNullOrderByUnblockedAtDesc(any())).thenReturn(Optional.empty());

            assertThat(service.selectCurrentQuest(userId, null)).contains(child);
        }
    }

    @Nested
    @DisplayName("Empty / no eligible")
    class EmptyEligible {

        @Test
        void emptyWhenNoPendingQuests() {
            when(questRepository.findByUserIdAndStatus(userId, QuestStatus.pending)).thenReturn(List.of());
            assertThat(service.selectCurrentQuest(userId, null)).isEmpty();
        }

        @Test
        void emptyWhenAllBlocked() {
            Quest q = quest(UUID.randomUUID(), userId, PriorityLevel.P0, null, QuestStatus.pending);
            when(questRepository.findByUserIdAndStatus(userId, QuestStatus.pending)).thenReturn(List.of(q));
            when(blockedQuestRepository.findByQuestIdAndUnblockedAtIsNull(q.getId()))
                    .thenReturn(Optional.of(blockedQuest(q.getId(), null)));
            when(blockedQuestRepository.findTopByQuestIdAndUnblockedAtIsNotNullOrderByUnblockedAtDesc(any())).thenReturn(Optional.empty());

            assertThat(service.selectCurrentQuest(userId, null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Reasons")
    class Reasons {

        @Test
        void highPriorityReasonForP0() {
            User u = user(userId, "UTC");
            Quest q = quest(UUID.randomUUID(), userId, PriorityLevel.P0, null, QuestStatus.pending);
            when(userRepository.findById(userId)).thenReturn(Optional.of(u));

            List<String> reasons = service.computeReasons(q, u, null, utc, Instant.now());
            assertThat(reasons).contains("high_priority");
        }

        @Test
        void matchesContextReason() {
            User u = user(userId, "UTC");
            Quest q = quest(UUID.randomUUID(), userId, PriorityLevel.P2, null, QuestStatus.pending);
            q.setContextTag(ContextTag.focus);
            when(userRepository.findById(userId)).thenReturn(Optional.of(u));

            List<String> reasons = service.computeReasons(q, u, ContextTag.focus, utc, Instant.now());
            assertThat(reasons).contains("matches_context");
        }
    }

    @Nested
    @DisplayName("User not found")
    class UserNotFound {

        @Test
        void throwsWhenUserNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.selectCurrentQuest(userId, null))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }
}
