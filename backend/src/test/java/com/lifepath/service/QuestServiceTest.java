package com.lifepath.service;

import com.lifepath.dto.CreateQuestRequest;
import com.lifepath.dto.UpdateQuestRequest;
import com.lifepath.exception.ConflictException;
import com.lifepath.exception.UnprocessableEntityException;
import com.lifepath.model.Quest;
import com.lifepath.model.User;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.lifepath.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestServiceTest {

    @Mock QuestRepository questRepository;
    @Mock BlockedQuestRepository blockedQuestRepository;
    @Mock UserRepository userRepository;
    @Mock PriorityAlgorithmService priorityAlgorithmService;

    QuestService service;

    UUID userId = UUID.randomUUID();
    UUID questId = UUID.randomUUID();
    User user;

    @BeforeEach
    void setUp() {
        service = new QuestService(questRepository, blockedQuestRepository, userRepository, priorityAlgorithmService);
        user = user(userId, "UTC");
    }

    @Nested
    @DisplayName("Create quest")
    class Create {

        @Test
        void createsQuestWithoutParent() {
            CreateQuestRequest req = new CreateQuestRequest();
            req.setTitle("New Quest");
            req.setNotes("notes");
            req.setPriorityLevel(PriorityLevel.P2);
            req.setParentId(null);

            Quest saved = new Quest();
            saved.setId(questId);
            saved.setUserId(userId);
            saved.setTitle(req.getTitle());
            saved.setStatus(QuestStatus.pending);
            when(questRepository.save(any(Quest.class))).thenAnswer(inv -> inv.getArgument(0));

            var result = service.createQuest(userId, req);

            assertThat(result.getTitle()).isEqualTo("New Quest");
            assertThat(result.getPriorityLevel()).isEqualTo(PriorityLevel.P2);
            verify(questRepository).save(any(Quest.class));
        }

        @Test
        void rejectsCircularParent() {
            CreateQuestRequest req = new CreateQuestRequest();
            req.setTitle("Cycle");
            req.setPriorityLevel(PriorityLevel.P2);
            req.setParentId(questId);

            Quest existing = quest(questId, userId, PriorityLevel.P2, questId, QuestStatus.pending);
            when(questRepository.findByIdAndUserId(questId, userId)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.createQuest(userId, req))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("circular");
        }
    }

    @Nested
    @DisplayName("Delete quest")
    class Delete {

        @Test
        void rejectsDeleteWhenHasChildren() {
            Quest q = quest(questId, userId, PriorityLevel.P2, null, QuestStatus.pending);
            when(questRepository.findByIdAndUserId(questId, userId)).thenReturn(Optional.of(q));
            when(questRepository.countByParentId(questId)).thenReturn(2L);

            assertThatThrownBy(() -> service.deleteQuest(questId, userId))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("dependent");
        }

        @Test
        void softDeletesWhenNoChildren() {
            Quest q = quest(questId, userId, PriorityLevel.P2, null, QuestStatus.pending);
            when(questRepository.findByIdAndUserId(questId, userId)).thenReturn(Optional.of(q));
            when(questRepository.countByParentId(questId)).thenReturn(0L);
            when(questRepository.save(any(Quest.class))).thenAnswer(inv -> inv.getArgument(0));

            service.deleteQuest(questId, userId);

            verify(questRepository).save(argThat(quest -> quest.getDeletedAt() != null));
        }
    }

    @Nested
    @DisplayName("Complete quest")
    class Complete {

        @Test
        void completesPendingQuestAndAwardsXp() {
            Quest q = quest(questId, userId, PriorityLevel.P2, null, QuestStatus.pending);
            when(questRepository.findByIdAndUserId(questId, userId)).thenReturn(Optional.of(q));
            when(questRepository.save(any(Quest.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            var result = service.completeQuest(questId, userId);

            assertThat(result.getXpEarned()).isEqualTo(10);
            assertThat(result.getQuest().getStatus()).isEqualTo(QuestStatus.completed);
        }

        @Test
        void rejectsCompleteWhenAlreadyCompleted() {
            Quest q = quest(questId, userId, PriorityLevel.P2, null, QuestStatus.completed);
            when(questRepository.findByIdAndUserId(questId, userId)).thenReturn(Optional.of(q));

            assertThatThrownBy(() -> service.completeQuest(questId, userId))
                    .isInstanceOf(UnprocessableEntityException.class)
                    .hasMessageContaining("already completed");
        }
    }
}
