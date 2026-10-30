package com.lifepath.scheduler;

import com.lifepath.model.BlockedQuest;
import com.lifepath.model.Quest;
import com.lifepath.model.enums.QuestStatus;
import com.lifepath.model.enums.UnblockTrigger;
import com.lifepath.repository.BlockedQuestRepository;
import com.lifepath.repository.QuestRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class TimeBasedUnblockJob {

    private final BlockedQuestRepository blockedQuestRepository;
    private final QuestRepository questRepository;

    public TimeBasedUnblockJob(BlockedQuestRepository blockedQuestRepository,
                              QuestRepository questRepository) {
        this.blockedQuestRepository = blockedQuestRepository;
        this.questRepository = questRepository;
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void processTimeBasedUnblocks() {
        Instant now = Instant.now();
        List<BlockedQuest> due = blockedQuestRepository
                .findByUnblockTriggerAndUnblockedAtIsNullAndUnblockAtBefore(UnblockTrigger.time, now);

        for (BlockedQuest bq : due) {
            bq.setUnblockedAt(now);
            blockedQuestRepository.save(bq);

            questRepository.findById(bq.getQuestId()).ifPresent(quest -> {
                quest.setStatus(QuestStatus.pending);
                questRepository.save(quest);
            });
        }
    }
}
