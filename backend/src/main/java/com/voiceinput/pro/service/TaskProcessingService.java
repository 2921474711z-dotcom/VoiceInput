package com.voiceinput.pro.service;

import com.voiceinput.pro.config.AppProperties;
import com.voiceinput.pro.model.entity.AppConfigEntity;
import com.voiceinput.pro.model.entity.ProcessingTaskEntity;
import com.voiceinput.pro.model.entity.UploadedAssetEntity;
import com.voiceinput.pro.model.enums.TaskStatus;
import com.voiceinput.pro.repository.ProcessingTaskRepository;
import com.voiceinput.pro.support.JsonSupport;
import com.voiceinput.pro.support.TextSupport;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskProcessingService {

    private final ProcessingTaskRepository processingTaskRepository;
    private final StorageService storageService;
    private final HotwordService hotwordService;
    private final OpenAiCompatibleClient openAiCompatibleClient;
    private final PromptService promptService;
    private final ConfigService configService;
    private final JsonSupport jsonSupport;
    private final AppProperties appProperties;

    @Transactional
    public void process(String taskId) {
        ProcessingTaskEntity task = processingTaskRepository.findById(taskId).orElse(null);
        if (task == null || task.getStatus() == TaskStatus.SUCCESS || task.getStatus() == TaskStatus.PROCESSING) {
            return;
        }

        try {
            task.setStatus(TaskStatus.PROCESSING);
            processingTaskRepository.save(task);

            AppConfigEntity executionConfig = configService.resolveExecutionConfig(task.getModelConfigSnapshot());

            String rawText = task.getRawText();
            long recognitionDurationMs = 0L;
            if (rawText == null || rawText.isBlank()) {
                UploadedAssetEntity asset = storageService.getAsset(task.getUploadId());
                File tempFile = downloadTempFile(asset);
                var transcription = openAiCompatibleClient.transcribe(tempFile, executionConfig);
                rawText = transcription.text();
                recognitionDurationMs = transcription.durationMs();
                tempFile.delete();
            }

            HotwordService.HotwordApplyResult hotwordResult = executionConfig.getHotwordEnabled()
                ? hotwordService.applyHotwords(rawText, task.getSceneType())
                : new HotwordService.HotwordApplyResult(rawText, List.of());

            String prompt = promptService.buildOptimizationPrompt(task.getSceneType(), executionConfig, rawText, hotwordResult.text());
            var optimized = openAiCompatibleClient.optimize(prompt, executionConfig);

            task.setRawText(rawText);
            task.setOptimizedText(TextSupport.normalizeWhitespace(optimized.optimizedText()));
            task.setMarkdownContent(TextSupport.normalizeWhitespace(optimized.markdown()));
            task.setTitle(optimized.title());
            task.setSummary(optimized.summary());
            task.setStatus(TaskStatus.SUCCESS);
            task.setRawWordCount(TextSupport.countMeaningfulCharacters(rawText));
            task.setOptimizedWordCount(TextSupport.countMeaningfulCharacters(optimized.optimizedText()));
            task.setHotwordHitCount(hotwordResult.matches().stream().mapToInt(HotwordService.Match::hitCount).sum());
            task.setRecognitionDurationMs(recognitionDurationMs);
            task.setOptimizationDurationMs(optimized.durationMs());
            task.setTotalDurationMs(recognitionDurationMs + optimized.durationMs());
            task.setEstimatedCost(calculateCost(task, optimized.promptTokens(), optimized.completionTokens()));
            task.setHotwordMatchesJson(jsonSupport.toJson(hotwordResult.matches()));
            if (task.getModelConfigSnapshot() == null || task.getModelConfigSnapshot().isBlank()) {
                task.setModelConfigSnapshot(jsonSupport.toJson(configService.getActive()));
            }
            task.setCompletedAt(LocalDateTime.now());
            processingTaskRepository.save(task);
        } catch (Exception ex) {
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage(ex.getMessage());
            processingTaskRepository.save(task);
        }
    }

    private File downloadTempFile(UploadedAssetEntity asset) throws Exception {
        File tempFile = File.createTempFile("voiceinput-", ".audio");
        try (InputStream inputStream = storageService.getObjectStream(asset.getObjectKey());
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            inputStream.transferTo(outputStream);
        }
        return tempFile;
    }

    private BigDecimal calculateCost(ProcessingTaskEntity task, int promptTokens, int completionTokens) {
        BigDecimal asrMinutes = BigDecimal.valueOf(task.getRecognitionDurationMs() == null ? 0 : task.getRecognitionDurationMs())
            .divide(new BigDecimal("60000"), 6, RoundingMode.HALF_UP);
        BigDecimal asrCost = asrMinutes.multiply(appProperties.getPricing().getAsrPricePerMinute());
        BigDecimal inputCost = BigDecimal.valueOf(promptTokens)
            .divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP)
            .multiply(appProperties.getPricing().getLlmInputPricePer1k());
        BigDecimal outputCost = BigDecimal.valueOf(completionTokens)
            .divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP)
            .multiply(appProperties.getPricing().getLlmOutputPricePer1k());
        return asrCost.add(inputCost).add(outputCost).setScale(4, RoundingMode.HALF_UP);
    }
}
