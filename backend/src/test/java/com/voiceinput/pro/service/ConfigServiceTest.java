package com.voiceinput.pro.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.voiceinput.pro.config.AppProperties;
import com.voiceinput.pro.model.dto.AppConfigRequest;
import com.voiceinput.pro.model.dto.ConfigTemplateRequest;
import com.voiceinput.pro.model.dto.ConfigTemplateResponse;
import com.voiceinput.pro.model.entity.AppConfigEntity;
import com.voiceinput.pro.model.entity.ConfigTemplateEntity;
import com.voiceinput.pro.repository.AppConfigRepository;
import com.voiceinput.pro.repository.ConfigTemplateRepository;
import com.voiceinput.pro.support.JsonSupport;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {

    @Mock
    private AppConfigRepository appConfigRepository;

    @Mock
    private ConfigTemplateRepository configTemplateRepository;

    @Mock
    private AppProperties appProperties;

    @Mock
    private JsonSupport jsonSupport;

    @InjectMocks
    private ConfigService configService;

    @Test
    void updateTemplateShouldPersistEditedTemplateConfig() {
        ConfigTemplateEntity entity = new ConfigTemplateEntity();
        entity.setId("tpl-1");
        entity.setName("旧模板");
        entity.setDescription("旧说明");
        entity.setIsDefaultTemplate(false);
        entity.setCreatedAt(LocalDateTime.now());

        when(configTemplateRepository.findById("tpl-1")).thenReturn(Optional.of(entity));
        when(configTemplateRepository.save(any(ConfigTemplateEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppConfigRequest editedConfig = new AppConfigRequest(
            "高精度语音识别 v3",
            "英文",
            "技术表达",
            "Markdown",
            "更准确",
            "正式表达增强 v2",
            "代码注释优化",
            "专业客观",
            "详细",
            false,
            "质量优先",
            "mimo-v2-omni",
            "mimo-v2.5"
        );

        ConfigTemplateResponse response = configService.updateTemplate(
            "tpl-1",
            new ConfigTemplateRequest("技术模板", "更新后的模板", editedConfig, false)
        );

        assertThat(response.name()).isEqualTo("技术模板");
        assertThat(response.description()).isEqualTo("更新后的模板");
        assertThat(response.config().recognitionModel()).isEqualTo("高精度语音识别 v3");
        assertThat(response.config().languageType()).isEqualTo("英文");
        assertThat(response.config().hotwordEnabled()).isFalse();
        assertThat(response.config().asrModelRoute()).isEqualTo("mimo-v2-omni");
        assertThat(response.config().llmModelRoute()).isEqualTo("mimo-v2.5");
    }

    @Test
    void applyTemplateShouldCopyTemplateIntoActiveConfig() {
        ConfigTemplateEntity template = new ConfigTemplateEntity();
        template.setId("tpl-sync");
        template.setName("同步模板");
        template.setRecognitionModel("高精度语音识别 v3");
        template.setLanguageType("英文");
        template.setDomainModel("技术表达");
        template.setOutputFormat("Markdown");
        template.setStabilityMode("更准确");
        template.setOptimizationModel("正式表达增强 v2");
        template.setOptimizationGoal("代码注释优化");
        template.setToneStyle("专业客观");
        template.setLengthPreference("详细");
        template.setHotwordEnabled(false);
        template.setCostMode("质量优先");
        template.setAsrModelRoute("mimo-v2-omni");
        template.setLlmModelRoute("mimo-v2.5");

        AppConfigEntity active = new AppConfigEntity();
        active.setId("active");
        active.setRecognitionModel("通用语音识别 v3");
        active.setLanguageType("中文（普通话）");
        active.setAsrModelRoute("mimo-v2.5");
        active.setLlmModelRoute("mimo-v2.5-pro");

        when(configTemplateRepository.findById("tpl-sync")).thenReturn(Optional.of(template));
        when(appConfigRepository.findById("active")).thenReturn(Optional.of(active));
        when(appConfigRepository.save(any(AppConfigEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = configService.applyTemplate("tpl-sync");

        assertThat(response.recognitionModel()).isEqualTo("高精度语音识别 v3");
        assertThat(response.languageType()).isEqualTo("英文");
        assertThat(response.domainModel()).isEqualTo("技术表达");
        assertThat(response.outputFormat()).isEqualTo("Markdown");
        assertThat(response.hotwordEnabled()).isFalse();
        assertThat(response.asrModelRoute()).isEqualTo("mimo-v2-omni");
        assertThat(response.llmModelRoute()).isEqualTo("mimo-v2.5");
    }
}
