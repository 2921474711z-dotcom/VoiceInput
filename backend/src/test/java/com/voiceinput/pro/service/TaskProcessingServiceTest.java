package com.voiceinput.pro.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceinput.pro.config.AppProperties;
import com.voiceinput.pro.model.entity.AppConfigEntity;
import com.voiceinput.pro.model.entity.ProcessingTaskEntity;
import com.voiceinput.pro.model.enums.SceneType;
import com.voiceinput.pro.model.enums.TaskStatus;
import com.voiceinput.pro.repository.ProcessingTaskRepository;
import com.voiceinput.pro.support.JsonSupport;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskProcessingServiceTest {

    @Mock
    private ProcessingTaskRepository processingTaskRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private HotwordService hotwordService;

    @Mock
    private OpenAiCompatibleClient openAiCompatibleClient;

    @Mock
    private PromptService promptService;

    @Mock
    private ConfigService configService;

    @Mock
    private SceneOutputValidator sceneOutputValidator;

    @Mock
    private SceneOutputFormatter sceneOutputFormatter;

    private final JsonSupport jsonSupport = new JsonSupport(new ObjectMapper());

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private TaskProcessingService taskProcessingService;

    @Test
    void processShouldRetryOnceWhenFirstOptimizationDoesNotMatchScene() {
        ProcessingTaskEntity task = pendingMeetingTask();
        AppConfigEntity config = executionConfig();
        AppProperties.Pricing pricing = new AppProperties.Pricing();

        when(processingTaskRepository.findById("task-1")).thenReturn(Optional.of(task));
        when(processingTaskRepository.save(any(ProcessingTaskEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(configService.resolveExecutionConfig("{}")).thenReturn(config);
        when(hotwordService.applyHotwords("今天讨论登录改造和发布时间", SceneType.MEETING_MINUTES))
            .thenReturn(new HotwordService.HotwordApplyResult("今天讨论登录改造和发布时间", List.of()));
        when(promptService.buildOptimizationPrompt(any(), any(), anyString(), anyString())).thenReturn("prompt");
        when(promptService.buildRetryPrompt(
            eq(SceneType.MEETING_MINUTES),
            eq(config),
            eq("今天讨论登录改造和发布时间"),
            eq("今天讨论登录改造和发布时间"),
            any(OpenAiCompatibleClient.OptimizationResult.class),
            any(SceneOutputValidator.ValidationResult.class)
        )).thenReturn("retry-prompt");
        when(openAiCompatibleClient.optimize("prompt", config))
            .thenReturn(new OpenAiCompatibleClient.OptimizationResult(
                "会议纪要",
                "缺少待办",
                "会议概述：今天讨论登录改造。\n核心结论：本周三发布。",
                "# 会议纪要",
                "第一次输出不完整",
                120,
                80,
                300
            ));
        when(openAiCompatibleClient.optimize("retry-prompt", config))
            .thenReturn(new OpenAiCompatibleClient.OptimizationResult(
                "会议纪要",
                "结构完整",
                "会议概述：今天讨论登录改造。\n核心结论：本周三发布。\n待办事项：周二完成回归测试。",
                "# 会议纪要\n\n## 会议概述\n今天讨论登录改造。\n\n## 核心结论\n本周三发布。\n\n## 待办事项\n- 周二完成回归测试。",
                "补齐了待办事项",
                150,
                100,
                360
            ));
        when(sceneOutputValidator.validate(
            eq(SceneType.MEETING_MINUTES),
            eq("会议概述：今天讨论登录改造。\n核心结论：本周三发布。"),
            eq("# 会议纪要")
        )).thenReturn(SceneOutputValidator.ValidationResult.failure("缺少必需结构：待办事项", "补齐待办事项并保持会议纪要结构"));
        when(sceneOutputValidator.validate(
            eq(SceneType.MEETING_MINUTES),
            eq("会议概述：今天讨论登录改造。\n核心结论：本周三发布。\n待办事项：周二完成回归测试。"),
            eq("# 会议纪要\n\n## 会议概述\n今天讨论登录改造。\n\n## 核心结论\n本周三发布。\n\n## 待办事项\n- 周二完成回归测试。")
        )).thenReturn(SceneOutputValidator.ValidationResult.success());
        when(sceneOutputFormatter.format(
            eq(SceneType.MEETING_MINUTES),
            eq("会议概述：今天讨论登录改造。\n核心结论：本周三发布。\n待办事项：周二完成回归测试。"),
            eq("# 会议纪要\n\n## 会议概述\n今天讨论登录改造。\n\n## 核心结论\n本周三发布。\n\n## 待办事项\n- 周二完成回归测试。")
        )).thenReturn(new SceneOutputFormatter.FormattedOutput(
            "会议概述：今天讨论登录改造。\n核心结论：本周三发布。\n待办事项：周二完成回归测试。",
            "# 会议纪要\n\n## 会议概述\n今天讨论登录改造。\n\n## 核心结论\n本周三发布。\n\n## 待办事项\n- 周二完成回归测试。"
        ));
        when(appProperties.getPricing()).thenReturn(pricing);

        taskProcessingService.process("task-1");

        verify(openAiCompatibleClient, times(2)).optimize(anyString(), eq(config));
        verify(promptService).buildRetryPrompt(
            eq(SceneType.MEETING_MINUTES),
            eq(config),
            eq("今天讨论登录改造和发布时间"),
            eq("今天讨论登录改造和发布时间"),
            any(OpenAiCompatibleClient.OptimizationResult.class),
            any(SceneOutputValidator.ValidationResult.class)
        );
    }

    private ProcessingTaskEntity pendingMeetingTask() {
        ProcessingTaskEntity task = new ProcessingTaskEntity();
        task.setId("task-1");
        task.setSceneType(SceneType.MEETING_MINUTES);
        task.setStatus(TaskStatus.PENDING);
        task.setFileName("meeting.wav");
        task.setRawText("今天讨论登录改造和发布时间");
        task.setModelConfigSnapshot("{}");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }

    private AppConfigEntity executionConfig() {
        AppConfigEntity config = new AppConfigEntity();
        config.setHotwordEnabled(true);
        config.setOptimizationGoal("会议纪要优化");
        config.setToneStyle("专业客观");
        config.setLengthPreference("适中");
        config.setOutputFormat("Markdown");
        config.setStabilityMode("平衡");
        return config;
    }
}
