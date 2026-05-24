package com.voiceinput.pro.model.enums;

public enum SceneType {
    MEETING_MINUTES("会议纪要"),
    WORK_REPORT("工作汇报"),
    FORMAL_EXPRESSION("正式表达"),
    MARKDOWN_NOTE("Markdown 笔记"),
    CODE_COMMENT("代码注释"),
    CHAT_REPLY("聊天回复");

    private final String label;

    SceneType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

