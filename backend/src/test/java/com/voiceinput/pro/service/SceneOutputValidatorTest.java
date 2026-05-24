package com.voiceinput.pro.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.voiceinput.pro.model.enums.SceneType;
import org.junit.jupiter.api.Test;

class SceneOutputValidatorTest {

    private final SceneOutputValidator validator = new SceneOutputValidator();

    @Test
    void validateShouldRejectMeetingMinutesWithoutTodoSection() {
        SceneOutputValidator.ValidationResult result = validator.validate(
            SceneType.MEETING_MINUTES,
            "会议概述：今天讨论登录改造。\n核心结论：本周三发布。",
            "# 会议纪要"
        );

        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("待办事项");
    }

    @Test
    void validateShouldAcceptMarkdownWithHeadingStructure() {
        SceneOutputValidator.ValidationResult result = validator.validate(
            SceneType.MARKDOWN_NOTE,
            "# 标题\n\n## 关键信息\n- A\n\n## 后续事项\n- B",
            "# 标题\n\n## 关键信息\n- A\n\n## 后续事项\n- B"
        );

        assertThat(result.passed()).isTrue();
    }

    @Test
    void validateShouldRequireWorkReportSections() {
        SceneOutputValidator.ValidationResult result = validator.validate(
            SceneType.WORK_REPORT,
            "当前进展：登录接口已完成联调。\n风险问题：回归测试排期紧张。\n下一步计划：明天下午完成验收。",
            "当前进展：登录接口已完成联调。\n风险问题：回归测试排期紧张。\n下一步计划：明天下午完成验收。"
        );

        assertThat(result.passed()).isTrue();
    }

    @Test
    void validateShouldRejectChatReplyWithHeadingAndTooManyLines() {
        SceneOutputValidator.ValidationResult result = validator.validate(
            SceneType.CHAT_REPLY,
            "# 回复\n- 第一条\n- 第二条\n- 第三条",
            "# 回复\n- 第一条\n- 第二条\n- 第三条"
        );

        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("聊天回复");
    }

    @Test
    void validateShouldAcceptFormalExpressionParagraph() {
        SceneOutputValidator.ValidationResult result = validator.validate(
            SceneType.FORMAL_EXPRESSION,
            "经综合评估，当前方案可以按计划推进，后续将根据测试结果继续优化。",
            "经综合评估，当前方案可以按计划推进，后续将根据测试结果继续优化。"
        );

        assertThat(result.passed()).isTrue();
    }

    @Test
    void validateShouldAcceptCodeCommentWithTechnicalTerms() {
        SceneOutputValidator.ValidationResult result = validator.validate(
            SceneType.CODE_COMMENT,
            "初始化 OAuth2 callback handler，避免 redirectUri 为空时触发 NPE。",
            "初始化 OAuth2 callback handler，避免 redirectUri 为空时触发 NPE。"
        );

        assertThat(result.passed()).isTrue();
    }
}
