package com.voiceinput.pro.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.voiceinput.pro.model.dto.ProofreadTaskRequest;
import com.voiceinput.pro.model.entity.ProcessingTaskEntity;
import com.voiceinput.pro.model.entity.ProofreadRevisionEntity;
import com.voiceinput.pro.model.enums.SceneType;
import com.voiceinput.pro.model.enums.TaskStatus;
import com.voiceinput.pro.repository.ProcessingTaskRepository;
import com.voiceinput.pro.repository.ProofreadRevisionRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProofreadServiceTest {

    @Mock
    private ProcessingTaskRepository processingTaskRepository;

    @Mock
    private ProofreadRevisionRepository proofreadRevisionRepository;

    @InjectMocks
    private ProofreadService proofreadService;

    @Test
    void saveProofreadShouldUpdateTaskAndCreateRevision() {
        ProcessingTaskEntity task = new ProcessingTaskEntity();
        task.setId("task-1");
        task.setSceneType(SceneType.WORK_REPORT);
        task.setStatus(TaskStatus.SUCCESS);
        task.setFileName("report.wav");
        task.setRawText("模型原始文本");
        task.setOptimizedText("模型优化文本");
        task.setMarkdownContent("# 模型优化文本");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        when(processingTaskRepository.findByIdAndDeletedFalse("task-1")).thenReturn(Optional.of(task));
        when(proofreadRevisionRepository.save(any(ProofreadRevisionEntity.class))).thenAnswer(invocation -> {
            ProofreadRevisionEntity entity = invocation.getArgument(0);
            entity.setId("revision-1");
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            return entity;
        });
        when(processingTaskRepository.save(any(ProcessingTaskEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = proofreadService.saveProofread(
            "task-1",
            new ProofreadTaskRequest("人工原始文本", "人工优化文本", "# 人工优化文本")
        );

        assertThat(response.proofreadRevisionId()).isEqualTo("revision-1");
        assertThat(response.rawText()).isEqualTo("人工原始文本");
        assertThat(response.optimizedText()).isEqualTo("人工优化文本");
        assertThat(task.getProofreadRawText()).isEqualTo("人工原始文本");
        assertThat(task.getProofreadOptimizedText()).isEqualTo("人工优化文本");
        assertThat(task.getProofreadMarkdownContent()).isEqualTo("# 人工优化文本");
    }
}
