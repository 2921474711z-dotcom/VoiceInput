package com.voiceinput.pro.model.entity;

import com.voiceinput.pro.model.enums.SceneType;
import com.voiceinput.pro.model.enums.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "processing_task")
public class ProcessingTaskEntity extends AuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SceneType sceneType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TaskStatus status;

    @Column(nullable = false)
    private String fileName;

    private String uploadId;

    private String templateId;

    private String templateName;

    private String sourceTaskId;

    private Integer versionIndex;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String rawText;

    @Column(columnDefinition = "TEXT")
    private String optimizedText;

    @Column(columnDefinition = "TEXT")
    private String markdownContent;

    private Integer rawWordCount;

    private Integer optimizedWordCount;

    private Integer hotwordHitCount;

    @Column(precision = 12, scale = 6)
    private BigDecimal estimatedCost;

    private Long recognitionDurationMs;

    private Long optimizationDurationMs;

    private Long totalDurationMs;

    @Column(nullable = false)
    private Boolean savedToHistory = Boolean.FALSE;

    @Column(nullable = false)
    private Boolean deleted = Boolean.FALSE;

    @Column(columnDefinition = "TEXT")
    private String modelConfigSnapshot;

    @Column(columnDefinition = "TEXT")
    private String hotwordMatchesJson;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String proofreadRawText;

    @Column(columnDefinition = "TEXT")
    private String proofreadOptimizedText;

    @Column(columnDefinition = "TEXT")
    private String proofreadMarkdownContent;

    private String proofreadRevisionId;

    private LocalDateTime proofreadAt;
}
