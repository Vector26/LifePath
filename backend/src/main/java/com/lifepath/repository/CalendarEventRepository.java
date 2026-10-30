package com.lifepath.repository;

import com.lifepath.model.CalendarEvent;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {

    List<CalendarEvent> findByUserIdAndStartTimeBetween(UUID userId, Instant start, Instant end);

    List<CalendarEvent> findByUserIdAndCompletedFalseAndStartTimeBetween(UUID userId, Instant start, Instant end);

    Optional<CalendarEvent> findByUserIdAndExternalId(UUID userId, String externalId);

    Optional<CalendarEvent> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    void deleteByUserIdAndStartTimeBetween(UUID userId, Instant start, Instant end);

    @Modifying
    @Query("""
        delete from CalendarEvent e
        where e.userId = :userId
          and e.startTime between :start and :end
          and e.externalId not in :externalIds
    """)
    void deleteStaleInRange(
        @Param("userId") UUID userId,
        @Param("start") Instant start,
        @Param("end") Instant end,
        @Param("externalIds") List<String> externalIds
    );
}
