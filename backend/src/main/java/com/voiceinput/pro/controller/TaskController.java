package com.voiceinput.pro.controller;

import com.voiceinput.pro.model.dto.CreateTaskRequest;
import com.voiceinput.pro.model.dto.CreateTextTaskRequest;
import com.voiceinput.pro.model.dto.HistoryPageResponse;
import com.voiceinput.pro.model.dto.ProofreadTaskRequest;
import com.voiceinput.pro.model.dto.ProofreadTaskResponse;
import com.voiceinput.pro.model.dto.ReoptimizeTaskRequest;
import com.voiceinput.pro.model.dto.TaskDetailResponse;
import com.voiceinput.pro.model.dto.TaskSummaryResponse;
import com.voiceinput.pro.model.enums.SceneType;
import com.voiceinput.pro.model.enums.TaskStatus;
import com.voiceinput.pro.service.TaskService;
import com.voiceinput.pro.service.ProofreadService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final ProofreadService proofreadService;

    @PostMapping("/tasks/process")
    public TaskSummaryResponse createTask(@Valid @RequestBody CreateTaskRequest request) {
        return taskService.create(request);
    }

    @PostMapping("/tasks/process-text")
    public TaskSummaryResponse createTextTask(@Valid @RequestBody CreateTextTaskRequest request) {
        return taskService.createFromText(request);
    }

    @GetMapping("/tasks/{id}")
    public TaskDetailResponse detail(@PathVariable String id) {
        return taskService.detail(id);
    }

    @PostMapping("/tasks/{id}/save")
    public TaskDetailResponse saveToHistory(@PathVariable String id) {
        return taskService.saveToHistory(id);
    }

    @PostMapping("/tasks/{id}/proofread")
    public ProofreadTaskResponse proofread(@PathVariable String id, @Valid @RequestBody ProofreadTaskRequest request) {
        return proofreadService.saveProofread(id, request);
    }

    @PostMapping("/tasks/{id}/reoptimize")
    public TaskSummaryResponse reoptimize(
        @PathVariable String id,
        @RequestBody(required = false) ReoptimizeTaskRequest request
    ) {
        return taskService.reoptimize(id, request == null ? new ReoptimizeTaskRequest(null) : request);
    }

    @GetMapping("/history")
    public HistoryPageResponse history(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) SceneType sceneType,
        @RequestParam(required = false) TaskStatus status,
        @RequestParam(required = false) LocalDate startDate,
        @RequestParam(required = false) LocalDate endDate,
        @RequestParam(required = false, defaultValue = "time") String sort,
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "10") int size
    ) {
        return taskService.history(keyword, sceneType, status, startDate, endDate, sort, page, size);
    }

    @GetMapping("/history/{id}")
    public TaskDetailResponse historyDetail(@PathVariable String id) {
        return taskService.detail(id);
    }

    @DeleteMapping("/history/{id}")
    public void deleteHistory(@PathVariable String id) {
        taskService.deleteHistory(id);
    }

    @GetMapping("/history/{id}/export-markdown")
    public ResponseEntity<ByteArrayResource> exportMarkdown(@PathVariable String id) {
        TaskService.MarkdownExportResult result = taskService.exportMarkdown(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.fileName() + "\"")
            .contentType(MediaType.parseMediaType("text/markdown; charset=utf-8"))
            .body(new ByteArrayResource(result.content()));
    }
}
