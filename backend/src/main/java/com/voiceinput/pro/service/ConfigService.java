package com.voiceinput.pro.service;

import com.voiceinput.pro.config.AppProperties;
import com.voiceinput.pro.model.dto.AppConfigRequest;
import com.voiceinput.pro.model.dto.AppConfigResponse;
import com.voiceinput.pro.model.dto.ConfigTemplateRequest;
import com.voiceinput.pro.model.dto.ConfigTemplateResponse;
import com.voiceinput.pro.model.entity.AppConfigEntity;
import com.voiceinput.pro.model.entity.ConfigTemplateEntity;
import com.voiceinput.pro.repository.AppConfigRepository;
import com.voiceinput.pro.repository.ConfigTemplateRepository;
import com.voiceinput.pro.support.ApiException;
import com.voiceinput.pro.support.JsonSupport;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfigService {

    private static final String ACTIVE_CONFIG_ID = "active";

    private final AppConfigRepository appConfigRepository;
    private final ConfigTemplateRepository configTemplateRepository;
    private final AppProperties appProperties;
    private final JsonSupport jsonSupport;

    public AppConfigEntity getActiveEntity() {
        return appConfigRepository.findById(ACTIVE_CONFIG_ID)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "未找到当前系统配置"));
    }

    public AppConfigResponse getActive() {
        return toResponse(getActiveEntity());
    }

    @Transactional
    public AppConfigResponse update(AppConfigRequest request) {
        AppConfigEntity entity = getActiveEntity();
        copy(request, entity);
        return toResponse(appConfigRepository.save(entity));
    }

    @Transactional
    public AppConfigResponse resetToDefault() {
        AppConfigEntity entity = getActiveEntity();
        entity.setRecognitionModel("通用语音识别 v3");
        entity.setLanguageType("中文（普通话）");
        entity.setDomainModel("通用领域");
        entity.setOutputFormat("纯文本");
        entity.setStabilityMode("平衡");
        entity.setOptimizationModel("文本优化增强 v2");
        entity.setOptimizationGoal("会议纪要优化");
        entity.setToneStyle("专业客观");
        entity.setLengthPreference("适中");
        entity.setHotwordEnabled(true);
        entity.setCostMode("成本优先");
        entity.setAsrModelRoute(defaultAsrModel());
        entity.setLlmModelRoute(defaultLlmModel());
        return toResponse(appConfigRepository.save(entity));
    }

    public List<ConfigTemplateResponse> listTemplates() {
        ensureDefaultTemplateExists();
        return configTemplateRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(this::toTemplateResponse)
            .toList();
    }

    @Transactional
    public ConfigTemplateResponse createTemplate(ConfigTemplateRequest request) {
        ConfigTemplateEntity entity = new ConfigTemplateEntity();
        entity.setName(request.name());
        entity.setDescription(request.description());
        copy(request.config(), entity);
        entity.setIsDefaultTemplate(false);
        ConfigTemplateEntity saved = configTemplateRepository.save(entity);

        if (Boolean.TRUE.equals(request.defaultTemplate())) {
            setDefaultTemplate(saved.getId());
            saved.setIsDefaultTemplate(true);
        }
        return toTemplateResponse(saved);
    }

    @Transactional
    public AppConfigResponse applyTemplate(String templateId) {
        ConfigTemplateEntity template = getTemplateEntity(templateId);
        AppConfigEntity entity = getActiveEntity();
        copy(template, entity);
        return toResponse(appConfigRepository.save(entity));
    }

    @Transactional
    public void setDefaultTemplate(String templateId) {
        ConfigTemplateEntity target = getTemplateEntity(templateId);
        clearDefaultTemplateFlags();
        target.setIsDefaultTemplate(true);

        AppConfigEntity entity = getActiveEntity();
        entity.setDefaultTemplateId(target.getId());
        appConfigRepository.save(entity);
        configTemplateRepository.save(target);
    }

    @Transactional
    public void deleteTemplate(String templateId) {
        ConfigTemplateEntity target = getTemplateEntity(templateId);
        AppConfigEntity entity = getActiveEntity();
        if (templateId.equals(entity.getDefaultTemplateId())) {
            entity.setDefaultTemplateId(null);
            appConfigRepository.save(entity);
        }
        configTemplateRepository.delete(target);
    }

    public TemplateResolution resolveTaskTemplate(String templateId) {
        ensureDefaultTemplateExists();
        if (templateId != null && !templateId.isBlank()) {
            ConfigTemplateEntity template = getTemplateEntity(templateId);
            return new TemplateResolution(template.getId(), template.getName(), toRequest(template));
        }

        AppConfigEntity active = getActiveEntity();
        if (active.getDefaultTemplateId() != null && !active.getDefaultTemplateId().isBlank()) {
            ConfigTemplateEntity template = getTemplateEntity(active.getDefaultTemplateId());
            return new TemplateResolution(template.getId(), template.getName(), toRequest(template));
        }

        ConfigTemplateEntity fallback = configTemplateRepository.findFirstByIsDefaultTemplateTrue();
        if (fallback != null) {
            return new TemplateResolution(fallback.getId(), fallback.getName(), toRequest(fallback));
        }

        throw new ApiException(HttpStatus.BAD_REQUEST, "当前没有可用模板，请先在配置模板中创建至少一套模板");
    }

    public AppConfigEntity resolveExecutionConfig(String snapshotJson) {
        if (snapshotJson == null || snapshotJson.isBlank()) {
            return getActiveEntity();
        }
        AppConfigRequest snapshot = jsonSupport.fromJson(snapshotJson, AppConfigRequest.class);
        return toExecutionEntity(snapshot);
    }

    private ConfigTemplateEntity getTemplateEntity(String templateId) {
        return configTemplateRepository.findById(templateId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "未找到配置模板"));
    }

    @Transactional
    protected void ensureDefaultTemplateExists() {
        if (configTemplateRepository.count() > 0) {
            return;
        }
        AppConfigEntity active = getActiveEntity();
        ConfigTemplateEntity template = new ConfigTemplateEntity();
        template.setName("系统默认模板");
        template.setDescription("由当前系统默认配置自动生成");
        copy(toRequest(active), template);
        template.setIsDefaultTemplate(true);
        ConfigTemplateEntity saved = configTemplateRepository.save(template);
        active.setDefaultTemplateId(saved.getId());
        appConfigRepository.save(active);
    }

    private void clearDefaultTemplateFlags() {
        configTemplateRepository.findAll().forEach(template -> template.setIsDefaultTemplate(false));
    }

    private AppConfigResponse toResponse(AppConfigEntity entity) {
        BigDecimal estimatedSeconds = switch (entity.getStabilityMode()) {
            case "更快" -> new BigDecimal("1.10");
            case "更准确" -> new BigDecimal("2.20");
            default -> new BigDecimal("1.35");
        };
        BigDecimal llmMinuteCost = appProperties.getPricing().getAsrPricePerMinute()
            .add(appProperties.getPricing().getLlmInputPricePer1k().multiply(new BigDecimal("8")))
            .add(appProperties.getPricing().getLlmOutputPricePer1k().multiply(new BigDecimal("4")));

        String defaultTemplateName = null;
        if (entity.getDefaultTemplateId() != null && !entity.getDefaultTemplateId().isBlank()) {
            defaultTemplateName = configTemplateRepository.findById(entity.getDefaultTemplateId())
                .map(ConfigTemplateEntity::getName)
                .orElse(null);
        }

        return new AppConfigResponse(
            entity.getRecognitionModel(),
            entity.getLanguageType(),
            entity.getDomainModel(),
            entity.getOutputFormat(),
            entity.getStabilityMode(),
            entity.getOptimizationModel(),
            entity.getOptimizationGoal(),
            entity.getToneStyle(),
            entity.getLengthPreference(),
            entity.getHotwordEnabled(),
            entity.getCostMode(),
            entity.getAsrModelRoute(),
            entity.getLlmModelRoute(),
            appProperties.getAsr().getProvider(),
            appProperties.getAsr().getBaseUrl(),
            entity.getAsrModelRoute(),
            appProperties.getLlm().getProvider(),
            appProperties.getLlm().getBaseUrl(),
            entity.getLlmModelRoute(),
            appProperties.getModel().getTimeoutSeconds(),
            appProperties.getModel().getMaxRetries(),
            estimatedSeconds,
            llmMinuteCost.setScale(4, RoundingMode.HALF_UP),
            entity.getDefaultTemplateId(),
            defaultTemplateName
        );
    }

    private ConfigTemplateResponse toTemplateResponse(ConfigTemplateEntity entity) {
        return new ConfigTemplateResponse(
            entity.getId(),
            entity.getName(),
            entity.getDescription(),
            entity.getIsDefaultTemplate(),
            toRequest(entity),
            entity.getCreatedAt()
        );
    }

    private AppConfigRequest toRequest(ConfigTemplateEntity entity) {
        return new AppConfigRequest(
            entity.getRecognitionModel(),
            entity.getLanguageType(),
            entity.getDomainModel(),
            entity.getOutputFormat(),
            entity.getStabilityMode(),
            entity.getOptimizationModel(),
            entity.getOptimizationGoal(),
            entity.getToneStyle(),
            entity.getLengthPreference(),
            entity.getHotwordEnabled(),
            entity.getCostMode(),
            entity.getAsrModelRoute(),
            entity.getLlmModelRoute()
        );
    }

    private AppConfigRequest toRequest(AppConfigEntity entity) {
        return new AppConfigRequest(
            entity.getRecognitionModel(),
            entity.getLanguageType(),
            entity.getDomainModel(),
            entity.getOutputFormat(),
            entity.getStabilityMode(),
            entity.getOptimizationModel(),
            entity.getOptimizationGoal(),
            entity.getToneStyle(),
            entity.getLengthPreference(),
            entity.getHotwordEnabled(),
            entity.getCostMode(),
            entity.getAsrModelRoute(),
            entity.getLlmModelRoute()
        );
    }

    private AppConfigEntity toExecutionEntity(AppConfigRequest request) {
        AppConfigEntity entity = new AppConfigEntity();
        entity.setId(ACTIVE_CONFIG_ID);
        copy(request, entity);
        return entity;
    }

    private void copy(AppConfigRequest source, AppConfigEntity target) {
        target.setRecognitionModel(source.recognitionModel());
        target.setLanguageType(source.languageType());
        target.setDomainModel(source.domainModel());
        target.setOutputFormat(source.outputFormat());
        target.setStabilityMode(source.stabilityMode());
        target.setOptimizationModel(source.optimizationModel());
        target.setOptimizationGoal(source.optimizationGoal());
        target.setToneStyle(source.toneStyle());
        target.setLengthPreference(source.lengthPreference());
        target.setHotwordEnabled(source.hotwordEnabled());
        target.setCostMode(source.costMode());
        target.setAsrModelRoute(source.asrModelRoute());
        target.setLlmModelRoute(source.llmModelRoute());
    }

    private void copy(ConfigTemplateEntity source, AppConfigEntity target) {
        target.setRecognitionModel(source.getRecognitionModel());
        target.setLanguageType(source.getLanguageType());
        target.setDomainModel(source.getDomainModel());
        target.setOutputFormat(source.getOutputFormat());
        target.setStabilityMode(source.getStabilityMode());
        target.setOptimizationModel(source.getOptimizationModel());
        target.setOptimizationGoal(source.getOptimizationGoal());
        target.setToneStyle(source.getToneStyle());
        target.setLengthPreference(source.getLengthPreference());
        target.setHotwordEnabled(source.getHotwordEnabled());
        target.setCostMode(source.getCostMode());
        target.setAsrModelRoute(source.getAsrModelRoute());
        target.setLlmModelRoute(source.getLlmModelRoute());
    }

    private void copy(AppConfigRequest source, ConfigTemplateEntity target) {
        target.setRecognitionModel(source.recognitionModel());
        target.setLanguageType(source.languageType());
        target.setDomainModel(source.domainModel());
        target.setOutputFormat(source.outputFormat());
        target.setStabilityMode(source.stabilityMode());
        target.setOptimizationModel(source.optimizationModel());
        target.setOptimizationGoal(source.optimizationGoal());
        target.setToneStyle(source.toneStyle());
        target.setLengthPreference(source.lengthPreference());
        target.setHotwordEnabled(source.hotwordEnabled());
        target.setCostMode(source.costMode());
        target.setAsrModelRoute(source.asrModelRoute());
        target.setLlmModelRoute(source.llmModelRoute());
    }

    private String defaultAsrModel() {
        return appProperties.getAsr().getModel() == null || appProperties.getAsr().getModel().isBlank()
            ? "mimo-v2.5"
            : appProperties.getAsr().getModel();
    }

    private String defaultLlmModel() {
        return appProperties.getLlm().getModel() == null || appProperties.getLlm().getModel().isBlank()
            ? "mimo-v2.5-pro"
            : appProperties.getLlm().getModel();
    }

    public record TemplateResolution(
        String templateId,
        String templateName,
        AppConfigRequest config
    ) {
    }
}
