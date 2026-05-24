package com.voiceinput.pro.service;

import com.voiceinput.pro.config.AppProperties;
import com.voiceinput.pro.model.dto.ModelConnectionTestResponse;
import com.voiceinput.pro.model.entity.AppConfigEntity;
import com.voiceinput.pro.model.entity.ModelConnectionTestEntity;
import com.voiceinput.pro.model.entity.UploadedAssetEntity;
import com.voiceinput.pro.repository.ModelConnectionTestRepository;
import com.voiceinput.pro.support.ApiException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ModelConnectionTestService {

    private final ConfigService configService;
    private final StorageService storageService;
    private final OpenAiCompatibleClient openAiCompatibleClient;
    private final ModelConnectionTestRepository modelConnectionTestRepository;
    private final AppProperties appProperties;

    @Transactional
    public ModelConnectionTestResponse testConnection(String target, String uploadId) {
        String normalizedTarget = target == null ? "" : target.trim().toUpperCase(Locale.ROOT);
        if (!"LLM".equals(normalizedTarget) && !"ASR".equals(normalizedTarget)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "连接测试目标只支持 LLM 或 ASR");
        }

        AppConfigEntity config = configService.getActiveEntity();
        ModelConnectionTestEntity entity = baseEntity(normalizedTarget, config, uploadId);
        try {
            if ("LLM".equals(normalizedTarget)) {
                var result = openAiCompatibleClient.testLlmConnection(config);
                entity.setStatus("SUCCESS");
                entity.setMessage(result.message());
                entity.setDurationMs(result.durationMs());
            } else if (uploadId == null || uploadId.isBlank()) {
                entity.setStatus("NEEDS_AUDIO");
                entity.setMessage("ASR 需要上传测试音频后才能进行真实识别测试，本次没有返回假成功。");
                entity.setDurationMs(0L);
            } else {
                UploadedAssetEntity asset = storageService.getAsset(uploadId);
                File tempFile = downloadTempFile(asset);
                try {
                    var result = openAiCompatibleClient.testAsrConnection(tempFile, config);
                    entity.setStatus("SUCCESS");
                    entity.setMessage(result.message());
                    entity.setDurationMs(result.durationMs());
                } finally {
                    tempFile.delete();
                }
            }
        } catch (Exception ex) {
            entity.setStatus("FAILED");
            entity.setMessage(ex.getMessage());
            entity.setDurationMs(entity.getDurationMs() == null ? 0L : entity.getDurationMs());
        }

        return toResponse(modelConnectionTestRepository.save(entity));
    }

    private ModelConnectionTestEntity baseEntity(String target, AppConfigEntity config, String uploadId) {
        ModelConnectionTestEntity entity = new ModelConnectionTestEntity();
        entity.setTarget(target);
        entity.setUploadId(uploadId);
        if ("ASR".equals(target)) {
            entity.setProvider(appProperties.getAsr().getProvider());
            entity.setBaseUrl(appProperties.getAsr().getBaseUrl());
            entity.setModelName(config.getAsrModelRoute());
        } else {
            entity.setProvider(appProperties.getLlm().getProvider());
            entity.setBaseUrl(appProperties.getLlm().getBaseUrl());
            entity.setModelName(config.getLlmModelRoute());
        }
        return entity;
    }

    private File downloadTempFile(UploadedAssetEntity asset) throws Exception {
        File tempFile = File.createTempFile("voiceinput-model-test-", ".audio");
        try (InputStream inputStream = storageService.getObjectStream(asset.getObjectKey());
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            inputStream.transferTo(outputStream);
        }
        return tempFile;
    }

    private ModelConnectionTestResponse toResponse(ModelConnectionTestEntity entity) {
        return new ModelConnectionTestResponse(
            entity.getId(),
            entity.getTarget(),
            entity.getProvider(),
            entity.getBaseUrl(),
            entity.getModelName(),
            entity.getStatus(),
            entity.getMessage(),
            entity.getDurationMs(),
            entity.getUploadId(),
            entity.getCreatedAt()
        );
    }
}
