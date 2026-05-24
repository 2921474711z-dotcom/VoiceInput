package com.voiceinput.pro.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.voiceinput.pro.model.dto.CreateExportRequest;
import com.voiceinput.pro.model.entity.ExportRecordEntity;
import com.voiceinput.pro.model.entity.ProcessingTaskEntity;
import com.voiceinput.pro.model.enums.SceneType;
import com.voiceinput.pro.model.enums.TaskStatus;
import com.voiceinput.pro.repository.ExportRecordRepository;
import com.voiceinput.pro.repository.ProcessingTaskRepository;
import com.voiceinput.pro.support.JsonSupport;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private ProcessingTaskRepository processingTaskRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private ExportRecordRepository exportRecordRepository;

    @Mock
    private JsonSupport jsonSupport;

    @InjectMocks
    private ExportService exportService;

    @Test
    void createDocxExportShouldPersistMinioBackedRecord() {
        ProcessingTaskEntity task = successfulTask();
        when(processingTaskRepository.findByIdAndDeletedFalse("task-1")).thenReturn(Optional.of(task));
        when(storageService.saveExport(any(), any(), any(), any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            ExportRecordEntity record = new ExportRecordEntity();
            record.setId("export-1");
            record.setTaskId(invocation.getArgument(0));
            record.setTaskTitle(invocation.getArgument(1));
            record.setFileName(invocation.getArgument(2));
            record.setObjectKey("exports/2026-05-24/file.docx");
            record.setExportType(invocation.getArgument(4));
            record.setContentSource(invocation.getArgument(5));
            record.setContentType(invocation.getArgument(6));
            record.setSizeBytes((long) ((byte[]) invocation.getArgument(3)).length);
            record.setStatus("SUCCESS");
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            return record;
        });

        var response = exportService.createExport(new CreateExportRequest("task-1", "DOCX", "PROOFREAD"));

        assertThat(response.id()).isEqualTo("export-1");
        assertThat(response.exportType()).isEqualTo("DOCX");
        assertThat(response.contentSource()).isEqualTo("PROOFREAD");
        assertThat(response.fileName()).endsWith(".docx");
        assertThat(response.sizeBytes()).isGreaterThan(0);
    }

    @Test
    void createJsonExportShouldIncludeTaskMetadata() {
        ProcessingTaskEntity task = successfulTask();
        when(processingTaskRepository.findByIdAndDeletedFalse("task-1")).thenReturn(Optional.of(task));
        when(jsonSupport.toJson(any())).thenReturn("{\"id\":\"task-1\",\"title\":\"会议纪要\"}");
        when(storageService.saveExport(any(), any(), any(), any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            ExportRecordEntity record = new ExportRecordEntity();
            record.setId("export-json");
            record.setTaskId(invocation.getArgument(0));
            record.setTaskTitle(invocation.getArgument(1));
            record.setFileName(invocation.getArgument(2));
            record.setObjectKey("exports/2026-05-24/file.json");
            record.setExportType(invocation.getArgument(4));
            record.setContentSource(invocation.getArgument(5));
            record.setContentType(invocation.getArgument(6));
            record.setSizeBytes((long) ((byte[]) invocation.getArgument(3)).length);
            record.setStatus("SUCCESS");
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            return record;
        });

        var response = exportService.createExport(new CreateExportRequest("task-1", "JSON", "MODEL"));

        assertThat(response.exportType()).isEqualTo("JSON");
        assertThat(response.fileName()).endsWith(".json");
        assertThat(response.contentType()).isEqualTo("application/json; charset=utf-8");
    }

    private ProcessingTaskEntity successfulTask() {
        ProcessingTaskEntity task = new ProcessingTaskEntity();
        task.setId("task-1");
        task.setTitle("会议纪要");
        task.setSceneType(SceneType.MEETING_MINUTES);
        task.setStatus(TaskStatus.SUCCESS);
        task.setFileName("meeting.wav");
        task.setRawText("原始识别内容");
        task.setOptimizedText("优化后的会议纪要");
        task.setMarkdownContent("# 会议纪要\n\n优化后的会议纪要");
        task.setProofreadOptimizedText("人工校对后的会议纪要");
        task.setProofreadMarkdownContent("# 会议纪要\n\n人工校对后的会议纪要");
        task.setSavedToHistory(true);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }
}
