package com.voiceinput.pro.service;

import com.voiceinput.pro.model.dto.CreateExportRequest;
import com.voiceinput.pro.model.dto.ExportRecordResponse;
import com.voiceinput.pro.model.entity.ExportRecordEntity;
import com.voiceinput.pro.model.entity.ProcessingTaskEntity;
import com.voiceinput.pro.repository.ExportRecordRepository;
import com.voiceinput.pro.repository.ProcessingTaskRepository;
import com.voiceinput.pro.support.ApiException;
import com.voiceinput.pro.support.JsonSupport;
import com.voiceinput.pro.support.TextSupport;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final ProcessingTaskRepository processingTaskRepository;
    private final StorageService storageService;
    private final ExportRecordRepository exportRecordRepository;
    private final JsonSupport jsonSupport;

    public List<ExportRecordResponse> list(String exportType) {
        String normalizedType = normalizeBlank(exportType);
        List<ExportRecordEntity> records = normalizedType == null
            ? exportRecordRepository.findAllByOrderByCreatedAtDesc()
            : exportRecordRepository.findByExportTypeOrderByCreatedAtDesc(normalizedType.toUpperCase(Locale.ROOT));
        return records.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ExportRecordResponse createExport(CreateExportRequest request) {
        ProcessingTaskEntity task = processingTaskRepository.findByIdAndDeletedFalse(request.taskId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "未找到可导出的任务"));
        if (task.getStatus() != com.voiceinput.pro.model.enums.TaskStatus.SUCCESS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "只有处理成功的任务可以导出");
        }

        ExportPayload payload = buildPayload(task, normalizeType(request.exportType()), normalizeSource(request.contentSource()));
        ExportRecordEntity record = storageService.saveExport(
            task.getId(),
            task.getTitle(),
            payload.fileName(),
            payload.bytes(),
            payload.exportType(),
            payload.contentSource(),
            payload.contentType(),
            "exports"
        );
        return toResponse(record);
    }

    public ExportDownload download(String id) {
        ExportRecordEntity record = exportRecordRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "未找到导出记录"));
        return new ExportDownload(record.getFileName(), record.getContentType(), storageService.readExportBytes(record));
    }

    private ExportPayload buildPayload(ProcessingTaskEntity task, String type, String source) {
        ExportContent content = resolveContent(task, source);
        String safeTitle = sanitizeFileName(task.getTitle() == null || task.getTitle().isBlank() ? TextSupport.buildTitle(content.optimizedText()) : task.getTitle());
        String prefix = "%s-%s".formatted(LocalDate.now(), safeTitle);
        return switch (type) {
            case "DOCX" -> new ExportPayload(prefix + ".docx", "DOCX", source, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", buildDocx(task, content));
            case "MARKDOWN" -> new ExportPayload(prefix + ".md", "MARKDOWN", source, "text/markdown; charset=utf-8", content.markdown().getBytes(StandardCharsets.UTF_8));
            case "TXT" -> new ExportPayload(prefix + ".txt", "TXT", source, "text/plain; charset=utf-8", content.optimizedText().getBytes(StandardCharsets.UTF_8));
            case "JSON" -> new ExportPayload(prefix + ".json", "JSON", source, "application/json; charset=utf-8", buildJson(task, content).getBytes(StandardCharsets.UTF_8));
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "不支持的导出格式：" + type);
        };
    }

    private ExportContent resolveContent(ProcessingTaskEntity task, String source) {
        if ("PROOFREAD".equals(source)) {
            String proofreadOptimized = normalizeBlank(task.getProofreadOptimizedText());
            if (proofreadOptimized == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "当前任务还没有人工校对版本，不能导出校对版");
            }
            return new ExportContent(
                valueOrBlank(task.getProofreadRawText()),
                proofreadOptimized,
                markdownOrFallback(task.getProofreadMarkdownContent(), proofreadOptimized)
            );
        }
        return new ExportContent(
            valueOrBlank(task.getRawText()),
            valueOrBlank(task.getOptimizedText()),
            markdownOrFallback(task.getMarkdownContent(), valueOrBlank(task.getOptimizedText()))
        );
    }

    private byte[] buildDocx(ProcessingTaskEntity task, ExportContent content) {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFParagraph title = document.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setText(task.getTitle() == null || task.getTitle().isBlank() ? "VoiceInput Pro 导出文档" : task.getTitle());

            addSection(document, "优化后文本", content.optimizedText());
            addSection(document, "原始识别文本", content.rawText());
            addSection(document, "Markdown 内容", content.markdown());

            document.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "生成 DOCX 文件失败：" + ex.getMessage());
        }
    }

    private void addSection(XWPFDocument document, String heading, String body) {
        XWPFParagraph h = document.createParagraph();
        XWPFRun headingRun = h.createRun();
        headingRun.setBold(true);
        headingRun.setFontSize(13);
        headingRun.setText(heading);

        String[] lines = valueOrBlank(body).split("\\R", -1);
        for (String line : lines) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setFontSize(11);
            run.setText(line);
        }
    }

    private String buildJson(ProcessingTaskEntity task, ExportContent content) {
        return jsonSupport.toJson(Map.ofEntries(
            Map.entry("taskId", task.getId()),
            Map.entry("title", valueOrBlank(task.getTitle())),
            Map.entry("sceneType", task.getSceneType().name()),
            Map.entry("fileName", task.getFileName()),
            Map.entry("rawText", content.rawText()),
            Map.entry("optimizedText", content.optimizedText()),
            Map.entry("markdownContent", content.markdown()),
            Map.entry("hotwordHitCount", task.getHotwordHitCount() == null ? 0 : task.getHotwordHitCount()),
            Map.entry("estimatedCost", task.getEstimatedCost() == null ? "0" : task.getEstimatedCost().toPlainString()),
            Map.entry("totalDurationMs", task.getTotalDurationMs() == null ? 0 : task.getTotalDurationMs()),
            Map.entry("modelConfigSnapshot", valueOrBlank(task.getModelConfigSnapshot())),
            Map.entry("hotwordMatchesJson", valueOrBlank(task.getHotwordMatchesJson()))
        ));
    }

    private String markdownOrFallback(String markdown, String optimizedText) {
        String normalized = normalizeBlank(markdown);
        return normalized == null ? "# " + TextSupport.buildTitle(optimizedText) + "\n\n" + optimizedText : normalized;
    }

    private String normalizeType(String type) {
        return valueOrBlank(type).trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeSource(String source) {
        String normalized = valueOrBlank(source).trim().toUpperCase(Locale.ROOT);
        if (!"MODEL".equals(normalized) && !"PROOFREAD".equals(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "导出内容来源只支持 MODEL 或 PROOFREAD");
        }
        return normalized;
    }

    private String sanitizeFileName(String value) {
        String sanitized = valueOrBlank(value).replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return sanitized.isBlank() ? "voiceinput-export" : sanitized;
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String valueOrBlank(String value) {
        return value == null ? "" : value;
    }

    private ExportRecordResponse toResponse(ExportRecordEntity record) {
        return new ExportRecordResponse(
            record.getId(),
            record.getTaskId(),
            record.getTaskTitle(),
            record.getFileName(),
            record.getExportType(),
            record.getContentType(),
            record.getContentSource(),
            record.getSizeBytes(),
            record.getStatus(),
            record.getErrorMessage(),
            record.getCreatedAt()
        );
    }

    public record ExportDownload(String fileName, String contentType, byte[] bytes) {
    }

    private record ExportContent(String rawText, String optimizedText, String markdown) {
    }

    private record ExportPayload(String fileName, String exportType, String contentSource, String contentType, byte[] bytes) {
    }
}
