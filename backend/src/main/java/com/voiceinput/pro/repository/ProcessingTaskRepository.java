package com.voiceinput.pro.repository;

import com.voiceinput.pro.model.entity.ProcessingTaskEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessingTaskRepository extends JpaRepository<ProcessingTaskEntity, String> {

    String HISTORY_FILTER = """
        from processing_task t
        where t.saved_to_history = true
          and t.deleted = false
          and (cast(:sceneType as text) is null or t.scene_type = cast(:sceneType as text))
          and (cast(:status as text) is null or t.status = cast(:status as text))
          and (cast(:keyword as text) is null
               or coalesce(t.title, '') ilike concat('%', cast(:keyword as text), '%')
               or coalesce(t.summary, '') ilike concat('%', cast(:keyword as text), '%')
               or coalesce(t.optimized_text, '') ilike concat('%', cast(:keyword as text), '%')
               or coalesce(t.raw_text, '') ilike concat('%', cast(:keyword as text), '%'))
          and (cast(:startTime as timestamp) is null or t.created_at >= cast(:startTime as timestamp))
          and (cast(:endTime as timestamp) is null or t.created_at <= cast(:endTime as timestamp))
        """;

    String HISTORY_COUNT = "select count(*) " + HISTORY_FILTER;

    Optional<ProcessingTaskEntity> findByIdAndDeletedFalse(String id);

    @Query(
        value = "select * " + HISTORY_FILTER + " order by t.created_at desc",
        countQuery = HISTORY_COUNT,
        nativeQuery = true
    )
    Page<ProcessingTaskEntity> searchHistoryByTime(
        @Param("sceneType") String sceneType,
        @Param("status") String status,
        @Param("keyword") String keyword,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        Pageable pageable
    );

    @Query(
        value = "select * " + HISTORY_FILTER + " order by t.total_duration_ms desc nulls last, t.created_at desc",
        countQuery = HISTORY_COUNT,
        nativeQuery = true
    )
    Page<ProcessingTaskEntity> searchHistoryByDuration(
        @Param("sceneType") String sceneType,
        @Param("status") String status,
        @Param("keyword") String keyword,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        Pageable pageable
    );

    @Query(
        value = "select * " + HISTORY_FILTER + " order by t.optimized_word_count desc nulls last, t.created_at desc",
        countQuery = HISTORY_COUNT,
        nativeQuery = true
    )
    Page<ProcessingTaskEntity> searchHistoryByWords(
        @Param("sceneType") String sceneType,
        @Param("status") String status,
        @Param("keyword") String keyword,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        Pageable pageable
    );

    List<ProcessingTaskEntity> findBySavedToHistoryTrueAndDeletedFalseAndCreatedAtBetween(
        LocalDateTime start,
        LocalDateTime end
    );

    List<ProcessingTaskEntity> findTop20BySavedToHistoryTrueAndDeletedFalseOrderByCompletedAtDesc();
}
