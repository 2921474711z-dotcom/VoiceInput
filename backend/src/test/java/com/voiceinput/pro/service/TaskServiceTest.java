package com.voiceinput.pro.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.voiceinput.pro.model.dto.AppConfigRequest;
import com.voiceinput.pro.model.dto.CreateTaskRequest;
import com.voiceinput.pro.model.dto.ReoptimizeTaskRequest;
import com.voiceinput.pro.model.entity.ProcessingTaskEntity;
import com.voiceinput.pro.model.entity.UploadedAssetEntity;
import com.voiceinput.pro.model.enums.SceneType;
import com.voiceinput.pro.model.enums.TaskStatus;
import com.voiceinput.pro.repository.ProcessingTaskRepository;
import com.voiceinput.pro.support.JsonSupport;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private ProcessingTaskRepository processingTaskRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private TaskQueueService taskQueueService;

    @Mock
    private ConfigService configService;

    @Mock
    private JsonSupport jsonSupport;

    @InjectMocks
    private TaskService taskService;

    @Captor
    private ArgumentCaptor<ProcessingTaskEntity> taskCaptor;

    @Test
    void createShouldPersistSelectedTemplateSnapshot() {
        UploadedAssetEntity asset = new UploadedAssetEntity();
        asset.setId("upload-1");
        asset.setOriginalFileName("demo.wav");

        AppConfigRequest templateConfig = new AppConfigRequest(
            "高精度语音识别 v3",
            "英文",
            "技术表达",
            "Markdown",
            "更准确",
            "正式表达增强 v2",
            "代码注释优化",
            "专业客观",
            "详细",
            true,
            "质量优先",
            "mimo-v2-omni",
            "mimo-v2.5-pro"
        );

        when(storageService.getAsset("upload-1")).thenReturn(asset);
        when(configService.resolveTaskTemplate("tpl-tech")).thenReturn(new ConfigService.TemplateResolution("tpl-tech", "英文技术模板", templateConfig));
        when(jsonSupport.toJson(templateConfig)).thenReturn("{\"template\":\"tech\"}");
        when(processingTaskRepository.save(any(ProcessingTaskEntity.class))).thenAnswer(invocation -> {
            ProcessingTaskEntity entity = invocation.getArgument(0);
            entity.setId("task-1");
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            return entity;
        });

        taskService.create(new CreateTaskRequest("upload-1", SceneType.CODE_COMMENT, "tpl-tech"));

        verify(processingTaskRepository).save(taskCaptor.capture());
        ProcessingTaskEntity saved = taskCaptor.getValue();
        assertThat(saved.getTemplateId()).isEqualTo("tpl-tech");
        assertThat(saved.getTemplateName()).isEqualTo("英文技术模板");
        assertThat(saved.getModelConfigSnapshot()).isEqualTo("{\"template\":\"tech\"}");
        assertThat(saved.getSceneType()).isEqualTo(SceneType.CODE_COMMENT);
    }

    @Test
    void reoptimizeShouldReuseSourceTemplateWhenNoOverrideProvided() {
        ProcessingTaskEntity source = new ProcessingTaskEntity();
        source.setId("task-source");
        source.setUploadId("upload-1");
        source.setSceneType(SceneType.MEETING_MINUTES);
        source.setStatus(TaskStatus.SUCCESS);
        source.setFileName("meeting.wav");
        source.setVersionIndex(1);
        source.setRawText("raw");
        source.setTemplateId("tpl-meeting");
        source.setTemplateName("会议纪要模板");
        source.setModelConfigSnapshot("{\"template\":\"meeting\"}");

        when(processingTaskRepository.findByIdAndDeletedFalse("task-source")).thenReturn(Optional.of(source));
        when(processingTaskRepository.save(any(ProcessingTaskEntity.class))).thenAnswer(invocation -> {
            ProcessingTaskEntity entity = invocation.getArgument(0);
            entity.setId("task-new");
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            return entity;
        });

        taskService.reoptimize("task-source", new ReoptimizeTaskRequest(null));

        verify(processingTaskRepository).save(taskCaptor.capture());
        ProcessingTaskEntity saved = taskCaptor.getValue();
        assertThat(saved.getTemplateId()).isEqualTo("tpl-meeting");
        assertThat(saved.getTemplateName()).isEqualTo("会议纪要模板");
        assertThat(saved.getModelConfigSnapshot()).isEqualTo("{\"template\":\"meeting\"}");
        assertThat(saved.getSourceTaskId()).isEqualTo("task-source");
    }
}
