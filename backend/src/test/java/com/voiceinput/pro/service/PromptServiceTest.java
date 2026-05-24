package com.voiceinput.pro.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.voiceinput.pro.model.entity.AppConfigEntity;
import com.voiceinput.pro.model.enums.SceneType;
import org.junit.jupiter.api.Test;

class PromptServiceTest {

    private final PromptService promptService = new PromptService();

    @Test
    void buildOptimizationPromptShouldForceMeetingMinutesStructure() {
        AppConfigEntity config = sampleConfig();

        String prompt = promptService.buildOptimizationPrompt(
            SceneType.MEETING_MINUTES,
            config,
            "今天讨论登录改造和发布时间",
            "今天讨论登录改造和发布时间"
        );

        assertThat(prompt).contains("会议概述");
        assertThat(prompt).contains("核心结论");
        assertThat(prompt).contains("待办事项");
    }

    @Test
    void buildOptimizationPromptShouldForceChatReplyLengthLimit() {
        AppConfigEntity config = sampleConfig();

        String prompt = promptService.buildOptimizationPrompt(
            SceneType.CHAT_REPLY,
            config,
            "登录做完了，下午再对首页方案",
            "登录做完了，下午再对首页方案"
        );

        assertThat(prompt).contains("1 到 3 句");
        assertThat(prompt).contains("禁止标题");
        assertThat(prompt).contains("禁止列表");
    }

    private AppConfigEntity sampleConfig() {
        AppConfigEntity entity = new AppConfigEntity();
        entity.setOptimizationGoal("会议纪要优化");
        entity.setToneStyle("专业客观");
        entity.setLengthPreference("适中");
        entity.setOutputFormat("Markdown");
        entity.setStabilityMode("平衡");
        return entity;
    }
}
