package com.voiceinput.pro.service;

import com.voiceinput.pro.model.entity.AppConfigEntity;
import com.voiceinput.pro.model.enums.SceneType;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PromptService {

    public String buildOptimizationPrompt(SceneType sceneType, AppConfigEntity config, String rawText, String correctedText) {
        String sceneLabel = sceneLabel(sceneType);
        String sceneInstruction = sceneInstruction(sceneType);
        String outputConstraint = outputConstraint(sceneType);
        String optimizationGoal = resolveOptimizationGoal(sceneType, config.getOptimizationGoal());

        return """
            你是中文办公与技术场景的语音文本优化助手。
            你必须输出严格 JSON，不允许输出 JSON 之外的解释。
            你必须优先服从当前场景要求，不能套用其他场景的输出结构。

            当前场景：%s
            场景要求：%s
            输出约束：%s
            优化目标：%s
            语气风格：%s
            长度偏好：%s
            输出格式：%s
            稳定性策略：%s

            原始识别文本：
            %s

            已应用热词后的文本：
            %s

            请返回如下 JSON：
            {
              "title": "简短标题",
              "summary": "一句摘要",
              "optimizedText": "优化后的正文",
              "markdown": "Markdown 内容",
              "improvement": "说明相对原文改善了什么"
            }
            """.formatted(
            sceneLabel,
            sceneInstruction,
            outputConstraint,
            optimizationGoal,
            config.getToneStyle(),
            config.getLengthPreference(),
            config.getOutputFormat(),
            config.getStabilityMode(),
            rawText,
            correctedText
        ).lines().collect(Collectors.joining("\n"));
    }

    private String sceneLabel(SceneType sceneType) {
        return switch (sceneType) {
            case MEETING_MINUTES -> "会议纪要";
            case WORK_REPORT -> "工作汇报";
            case FORMAL_EXPRESSION -> "正式表达";
            case MARKDOWN_NOTE -> "Markdown 笔记";
            case CODE_COMMENT -> "代码注释";
            case CHAT_REPLY -> "聊天回复";
        };
    }

    private String sceneInstruction(SceneType sceneType) {
        return switch (sceneType) {
            case MEETING_MINUTES -> "按会议纪要格式输出，必须包含会议概述、核心结论、待办事项。";
            case WORK_REPORT -> "按工作汇报格式输出，按点归纳工作进展、风险、下一步安排。";
            case FORMAL_EXPRESSION -> "输出为正式表达，减少口语化，保持严谨、精炼和完整。";
            case MARKDOWN_NOTE -> "输出必须是结构化 Markdown 笔记，适合直接保存到知识库。";
            case CODE_COMMENT -> "输出面向技术文档和代码注释，保留术语、英文缩写和关键技术名词。";
            case CHAT_REPLY -> "输出简洁自然，适合直接发送给他人的即时聊天回复。";
        };
    }

    private String outputConstraint(SceneType sceneType) {
        return switch (sceneType) {
            case MEETING_MINUTES -> "必须保留“会议概述 / 核心结论 / 待办事项”结构，不要写成聊天回复或技术注释。";
            case WORK_REPORT -> "必须突出“进展 / 风险 / 下一步”，不要写成会议纪要。";
            case FORMAL_EXPRESSION -> "必须写成连贯正式文本，不要输出列表化会议纪要。";
            case MARKDOWN_NOTE -> "必须输出 Markdown 标题、二级标题或列表，不能只给纯段落。";
            case CODE_COMMENT -> "必须保留技术词汇和英文术语，不能为追求通顺而改写掉关键名词。";
            case CHAT_REPLY -> "必须短、自然、像人发消息，不能输出纪要、报告或 Markdown 大纲。";
        };
    }

    private String resolveOptimizationGoal(SceneType sceneType, String configuredGoal) {
        String sceneGoal = switch (sceneType) {
            case MEETING_MINUTES -> "提炼会议主题、结论和待办，便于会后同步。";
            case WORK_REPORT -> "归纳当前进展、风险阻塞和下一步计划，便于上报。";
            case FORMAL_EXPRESSION -> "统一表达风格，提升正式程度和可读性。";
            case MARKDOWN_NOTE -> "整理为层次清晰、可归档的 Markdown 笔记。";
            case CODE_COMMENT -> "保留技术细节并整理为适合代码注释或技术文档的表达。";
            case CHAT_REPLY -> "压缩为自然、直接、可立即发送的回复内容。";
        };

        if (configuredGoal == null || configuredGoal.isBlank()) {
            return sceneGoal;
        }
        if (!SceneType.MEETING_MINUTES.equals(sceneType) && "会议纪要优化".equals(configuredGoal)) {
            return sceneGoal + " 当前任务不是会议纪要，禁止套用会议纪要结构。";
        }
        return sceneGoal + " 额外要求：" + configuredGoal;
    }
}
