package com.voiceinput.pro.service;

import com.voiceinput.pro.model.enums.SceneType;
import org.springframework.stereotype.Service;

@Service
public class SceneOutputValidator {

    public ValidationResult validate(SceneType sceneType, String optimizedText, String markdown) {
        return switch (sceneType) {
            case MEETING_MINUTES -> requireAll(
                optimizedText,
                "会议概述",
                "核心结论",
                "待办事项"
            );
            case WORK_REPORT -> requireAll(
                optimizedText,
                "当前进展",
                "风险问题",
                "下一步计划"
            );
            case FORMAL_EXPRESSION -> requireFormalExpression(optimizedText);
            case MARKDOWN_NOTE -> requireMarkdown(markdown);
            case CODE_COMMENT -> requireCodeComment(optimizedText);
            case CHAT_REPLY -> requireChatReply(optimizedText);
        };
    }

    private ValidationResult requireAll(String content, String... requiredSections) {
        String safeContent = content == null ? "" : content;
        for (String requiredSection : requiredSections) {
            if (!safeContent.contains(requiredSection)) {
                return ValidationResult.failure("缺少必需结构：" + requiredSection, "补齐" + requiredSection + "并保持当前场景结构");
            }
        }
        return ValidationResult.success();
    }

    private ValidationResult requireMarkdown(String markdown) {
        String safeMarkdown = markdown == null ? "" : markdown;
        boolean hasHeading = safeMarkdown.contains("# ");
        boolean hasSubHeading = safeMarkdown.contains("## ");
        boolean hasList = safeMarkdown.contains("- ") || safeMarkdown.contains("* ");
        if (hasHeading && (hasSubHeading || hasList)) {
            return ValidationResult.success();
        }
        return ValidationResult.failure("Markdown 笔记缺少标题层级或列表结构", "至少补齐一级标题以及二级标题或列表");
    }

    private ValidationResult requireChatReply(String optimizedText) {
        String safeText = optimizedText == null ? "" : optimizedText.trim();
        if (safeText.contains("#") || safeText.contains("- ") || safeText.contains("* ")) {
            return ValidationResult.failure("聊天回复不应包含标题或列表结构", "改为 1 到 3 句自然对话文本，禁止标题和列表");
        }
        String[] lines = safeText.isBlank() ? new String[0] : safeText.split("\\R+");
        if (lines.length == 0 || lines.length > 3) {
            return ValidationResult.failure("聊天回复应控制在 1 到 3 句", "压缩为简短自然的 1 到 3 句回复");
        }
        return ValidationResult.success();
    }

    private ValidationResult requireFormalExpression(String optimizedText) {
        String safeText = optimizedText == null ? "" : optimizedText.trim();
        if (safeText.isBlank()) {
            return ValidationResult.failure("正式表达结果不能为空", "输出完整、连贯的正式表达段落");
        }
        if (safeText.contains("#") || safeText.contains("- ") || safeText.contains("* ")) {
            return ValidationResult.failure("正式表达不应包含标题或列表", "改为连贯正式段落，不要使用 Markdown 标题或列表");
        }
        return ValidationResult.success();
    }

    private ValidationResult requireCodeComment(String optimizedText) {
        String safeText = optimizedText == null ? "" : optimizedText.trim();
        if (safeText.isBlank()) {
            return ValidationResult.failure("代码注释结果不能为空", "补充包含技术术语的代码注释说明");
        }
        if (safeText.contains("会议概述") || safeText.contains("待办事项") || safeText.contains("当前进展")) {
            return ValidationResult.failure("代码注释不应沿用会议纪要或汇报结构", "改为技术说明，保留变量名、接口名或英文术语");
        }
        if (!safeText.matches(".*[A-Za-z].*")) {
            return ValidationResult.failure("代码注释缺少技术术语或英文标识", "至少保留一个英文变量名、接口名或技术缩写");
        }
        return ValidationResult.success();
    }

    public record ValidationResult(boolean passed, String reason, String repairHint) {

        public static ValidationResult success() {
            return new ValidationResult(true, "", "");
        }

        public static ValidationResult failure(String reason, String repairHint) {
            return new ValidationResult(false, reason, repairHint);
        }
    }
}
