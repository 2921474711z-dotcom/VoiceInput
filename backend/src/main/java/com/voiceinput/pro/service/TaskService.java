package com.voiceinput.pro.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.voiceinput.pro.model.dto.CreateTaskRequest;
import com.voiceinput.pro.model.dto.HistoryPageResponse;
import com.voiceinput.pro.model.dto.HotwordMatchResponse;
import com.voiceinput.pro.model.dto.ReoptimizeTaskRequest;
import com.voiceinput.pro.model.dto.TaskDetailResponse;
import com.voiceinput.pro.model.dto.TaskSummaryResponse;
import com.voiceinput.pro.model.entity.ProcessingTaskEntity;
import com.voiceinput.pro.model.entity.UploadedAssetEntity;
import com.voiceinput.pro.model.enums.SceneType;
import com.voiceinput.pro.model.enums.TaskStatus;
import com.voiceinput.pro.repository.ProcessingTaskRepository;
import com.voiceinput.pro.support.ApiException;
import com.voiceinput.pro.support.JsonSupport;
import com.voiceinput.pro.support.TextSupport;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final ProcessingTaskRepository processingTaskRepository;
    private final StorageService storageService;
    private final TaskQueueService taskQueueService;
    private final ConfigService configService;
    private final JsonSupport jsonSupport;

    @Transactional
    public TaskSummaryResponse create(CreateTaskRequest request) {
        UploadedAssetEntity asset = storageService.getAsset(request.uploadId());
        ConfigService.TemplateResolution template = configService.resolveTaskTemplate(request.templateId());

        ProcessingTaskEntity task = new ProcessingTaskEntity();
        task.setUploadId(asset.getId());
        task.setSceneType(request.sceneType());
        task.setStatus(TaskStatus.PENDING);
        task.setFileName(asset.getOriginalFileName());
        task.setTemplateId(template.templateId());
        task.setTemplateName(template.templateName());
        task.setVersionIndex(1);
        task.setSavedToHistory(false);
        task.setModelConfigSnapshot(jsonSupport.toJson(template.config()));

        ProcessingTaskEntity saved = processingTaskRepository.save(task);
        taskQueueService.enqueue(saved.getId());
        return toSummary(saved);
    }

    public TaskDetailResponse detail(String id) {
        ProcessingTaskEntity task = processingTaskRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "未找到任务"));
        return toDetail(task);
    }

    @Transactional
    public TaskDetailResponse saveToHistory(String id) {
        ProcessingTaskEntity task = processingTaskRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "未找到任务"));
        task.setSavedToHistory(true);
        return toDetail(processingTaskRepository.save(task));
    }

    @Transactional
    public TaskSummaryResponse reoptimize(String id, ReoptimizeTaskRequest request) {
        ProcessingTaskEntity source = processingTaskRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "未找到源任务"));

        String templateId = request == null ? null : blankToNull(request.templateId());
        String resolvedTemplateId = source.getTemplateId();
        String resolvedTemplateName = source.getTemplateName();
        String snapshot = source.getModelConfigSnapshot();

        if (templateId != null) {
            ConfigService.TemplateResolution selected = configService.resolveTaskTemplate(templateId);
            resolvedTemplateId = selected.templateId();
            resolvedTemplateName = selected.templateName();
            snapshot = jsonSupport.toJson(selected.config());
        }

        ProcessingTaskEntity task = new ProcessingTaskEntity();
        task.setUploadId(source.getUploadId());
        task.setSceneType(source.getSceneType());
        task.setStatus(TaskStatus.PENDING);
        task.setFileName(source.getFileName());
        task.setTemplateId(resolvedTemplateId);
        task.setTemplateName(resolvedTemplateName);
        task.setVersionIndex(source.getVersionIndex() == null ? 2 : source.getVersionIndex() + 1);
        task.setSourceTaskId(source.getId());
        task.setRawText(source.getRawText());
        task.setSavedToHistory(true);
        task.setModelConfigSnapshot(snapshot);

        ProcessingTaskEntity saved = processingTaskRepository.save(task);
        taskQueueService.enqueue(saved.getId());
        return toSummary(saved);
    }

    public HistoryPageResponse history(
        String keyword,
        SceneType sceneType,
        TaskStatus status,
        LocalDate startDate,
        LocalDate endDate,
        String sortKey,
        int page,
        int size
    ) {
        String normalizedSortKey = normalizeSortKey(sortKey);
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime start = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime end = endDate == null ? null : endDate.atTime(LocalTime.MAX);
        String sceneTypeValue = sceneType == null ? null : sceneType.name();
        String statusValue = status == null ? null : status.name();
        String keywordValue = blankToNull(keyword);

        var result = switch (normalizedSortKey) {
            case "duration" -> processingTaskRepository.searchHistoryByDuration(sceneTypeValue, statusValue, keywordValue, start, end, pageable);
            case "words" -> processingTaskRepository.searchHistoryByWords(sceneTypeValue, statusValue, keywordValue, start, end, pageable);
            default -> processingTaskRepository.searchHistoryByTime(sceneTypeValue, statusValue, keywordValue, start, end, pageable);
        };

        return new HistoryPageResponse(
            result.getContent().stream().map(this::toSummary).toList(),
            result.getTotalElements(),
            page,
            size
        );
    }

    @Transactional
    public void deleteHistory(String id) {
        ProcessingTaskEntity task = processingTaskRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "未找到记录"));
        task.setDeleted(true);
        processingTaskRepository.save(task);
    }

    @Transactional
    public MarkdownExportResult exportMarkdown(String id) {
        ProcessingTaskEntity task = processingTaskRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "未找到记录"));
        String content = task.getMarkdownContent();
        if (content == null || content.isBlank()) {
            content = "# " + TextSupport.buildTitle(task.getOptimizedText()) + "\n\n" + task.getOptimizedText();
        }
        String fileName = "%s-%s.md".formatted(LocalDate.now(), id);
        storageService.saveMarkdownExport(id, fileName, content);
        return new MarkdownExportResult(fileName, content.getBytes(StandardCharsets.UTF_8));
    }

    private String normalizeSortKey(String sortKey) {
        if ("duration".equalsIgnoreCase(sortKey)) {
            return "duration";
        }
        if ("words".equalsIgnoreCase(sortKey)) {
            return "words";
        }
        return "time";
    }

    private TaskSummaryResponse toSummary(ProcessingTaskEntity task) {
        return new TaskSummaryResponse(
            task.getId(),
            task.getTitle(),
            task.getSceneType(),
            task.getStatus(),
            task.getFileName(),
            task.getTemplateId(),
            task.getTemplateName(),
            task.getOptimizedWordCount(),
            task.getRawWordCount(),
            task.getHotwordHitCount(),
            task.getEstimatedCost(),
            task.getTotalDurationMs(),
            task.getSavedToHistory(),
            task.getCreatedAt(),
            task.getCompletedAt()
        );
    }

    private TaskDetailResponse toDetail(ProcessingTaskEntity task) {
        List<HotwordMatchResponse> matches;
        if (task.getHotwordMatchesJson() == null || task.getHotwordMatchesJson().isBlank()) {
            matches = Collections.emptyList();
        } else {
            List<HotwordService.Match> parsed = jsonSupport.fromJson(task.getHotwordMatchesJson(), new TypeReference<List<HotwordService.Match>>() {
            });
            matches = parsed.stream()
                .map(item -> new HotwordMatchResponse(item.recognizedTerm(), item.standardTerm(), item.hitCount()))
                .toList();
        }

        return new TaskDetailResponse(
            task.getId(),
            task.getSourceTaskId(),
            task.getVersionIndex(),
            task.getTitle(),
            task.getSummary(),
            task.getSceneType(),
            task.getStatus(),
            task.getFileName(),
            task.getTemplateId(),
            task.getTemplateName(),
            task.getRawText(),
            task.getOptimizedText(),
            task.getMarkdownContent(),
            task.getRawWordCount(),
            task.getOptimizedWordCount(),
            task.getHotwordHitCount(),
            task.getEstimatedCost(),
            task.getRecognitionDurationMs(),
            task.getOptimizationDurationMs(),
            task.getTotalDurationMs(),
            task.getSavedToHistory(),
            task.getErrorMessage(),
            task.getCreatedAt(),
            task.getCompletedAt(),
            matches
        );
    }

    private String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    public record MarkdownExportResult(String fileName, byte[] content) {
    }
}
