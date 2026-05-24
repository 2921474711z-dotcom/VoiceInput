package com.voiceinput.pro.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.voiceinput.pro.model.entity.AppConfigEntity;
import com.voiceinput.pro.repository.ModelConnectionTestRepository;
import com.voiceinput.pro.config.AppProperties;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModelConnectionTestServiceTest {

    @Mock
    private ConfigService configService;

    @Mock
    private StorageService storageService;

    @Mock
    private OpenAiCompatibleClient openAiCompatibleClient;

    @Mock
    private ModelConnectionTestRepository modelConnectionTestRepository;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private ModelConnectionTestService modelConnectionTestService;

    @Test
    void testLlmShouldCallRealClientProbeAndPersistResult() {
        AppConfigEntity config = activeConfig();
        configureLlmProperties();
        when(configService.getActiveEntity()).thenReturn(config);
        when(openAiCompatibleClient.testLlmConnection(config)).thenReturn(new OpenAiCompatibleClient.ConnectionProbeResult(true, "模型响应正常", 123));
        when(modelConnectionTestRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            var entity = (com.voiceinput.pro.model.entity.ModelConnectionTestEntity) invocation.getArgument(0);
            entity.setId("test-1");
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            return entity;
        });

        var response = modelConnectionTestService.testConnection("LLM", null);

        assertThat(response.target()).isEqualTo("LLM");
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.message()).contains("模型响应正常");
        assertThat(response.durationMs()).isEqualTo(123);
    }

    @Test
    void testAsrWithoutUploadShouldNotReportFakeSuccess() {
        configureAsrProperties();
        when(configService.getActiveEntity()).thenReturn(activeConfig());
        when(modelConnectionTestRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            var entity = (com.voiceinput.pro.model.entity.ModelConnectionTestEntity) invocation.getArgument(0);
            entity.setId("test-asr");
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            return entity;
        });

        var response = modelConnectionTestService.testConnection("ASR", null);

        assertThat(response.target()).isEqualTo("ASR");
        assertThat(response.status()).isEqualTo("NEEDS_AUDIO");
        assertThat(response.message()).contains("上传测试音频");
    }

    private AppConfigEntity activeConfig() {
        AppConfigEntity entity = new AppConfigEntity();
        entity.setId("active");
        entity.setAsrModelRoute("mimo-v2.5");
        entity.setLlmModelRoute("mimo-v2.5-pro");
        return entity;
    }

    private void configureAsrProperties() {
        AppProperties.ModelEndpoint asr = new AppProperties.ModelEndpoint();
        asr.setProvider("xiaomi-mimo");
        asr.setBaseUrl("https://token-plan-cn.xiaomimimo.com");
        when(appProperties.getAsr()).thenReturn(asr);
    }

    private void configureLlmProperties() {
        AppProperties.ModelEndpoint llm = new AppProperties.ModelEndpoint();
        llm.setProvider("xiaomi-mimo");
        llm.setBaseUrl("https://token-plan-cn.xiaomimimo.com");
        when(appProperties.getLlm()).thenReturn(llm);
    }
}
