package com.voiceinput.pro.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceinput.pro.config.AppProperties;
import com.voiceinput.pro.model.entity.AppConfigEntity;
import com.voiceinput.pro.support.ApiException;
import com.voiceinput.pro.support.TextSupport;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.net.ssl.SSLException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

@Service
@RequiredArgsConstructor
public class OpenAiCompatibleClient {

    private static final int XIAOMI_AUDIO_BASE64_LIMIT = 50 * 1024 * 1024;

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TranscriptionResult transcribe(File file, AppConfigEntity currentConfig) {
        AppProperties.ModelEndpoint config = resolveAsrEndpoint(currentConfig);
        validate(config.getApiKey(), "ASR_API_KEY 未配置");

        String provider = normalizedProvider(config);
        if ("xiaomi-mimo".equals(provider) || isXiaomiAudioUnderstanding(config)) {
            return transcribeByXiaomiAudioUnderstanding(file, config, currentConfig);
        }
        return transcribeByOpenAiAudioApi(file, config, currentConfig);
    }

    public OptimizationResult optimize(String prompt, AppConfigEntity currentConfig) {
        AppProperties.ModelEndpoint config = resolveLlmEndpoint(currentConfig);
        validate(config.getApiKey(), "LLM_API_KEY 未配置");
        Instant start = Instant.now();

        Map<String, Object> body = Map.of(
            "model", config.getModel(),
            "response_format", Map.of("type", "json_object"),
            "max_completion_tokens", resolveOptimizationTokenBudget(currentConfig),
            "messages", List.of(
                Map.of("role", "system", "content", buildOptimizationSystemPrompt(currentConfig)),
                Map.of("role", "user", "content", prompt)
            )
        );

        Map<?, ?> response = client(config)
            .post()
            .uri(buildV1Uri(config, "/chat/completions"))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .onStatus(HttpStatusCode::isError, clientResponse -> mapModelError(clientResponse, "调用大模型失败"))
            .bodyToMono(Map.class)
            .onErrorResume(ex -> Mono.error(wrapModelException(ex, "调用大模型失败")))
            .retryWhen(retrySpec("调用大模型失败"))
            .block(requestTimeout());

        String content = extractMessageText(firstMessage(response));
        Map<String, Object> json;
        try {
            json = objectMapper.readValue(content, Map.class);
        } catch (Exception ex) {
            json = Map.of(
                "title", TextSupport.buildTitle(content),
                "summary", "模型返回了非 JSON 结构，已按纯文本回退处理。",
                "optimizedText", content,
                "markdown", content,
                "improvement", "已完成基础文本优化。"
            );
        }

        Map<?, ?> usage = response == null ? null : (Map<?, ?>) response.get("usage");
        int promptTokens = usage != null && usage.containsKey("prompt_tokens")
            ? ((Number) usage.get("prompt_tokens")).intValue()
            : 0;
        int completionTokens = usage != null && usage.containsKey("completion_tokens")
            ? ((Number) usage.get("completion_tokens")).intValue()
            : 0;

        return new OptimizationResult(
            String.valueOf(json.getOrDefault("title", TextSupport.buildTitle(content))),
            String.valueOf(json.getOrDefault("summary", "")),
            String.valueOf(json.getOrDefault("optimizedText", content)),
            String.valueOf(json.getOrDefault("markdown", content)),
            String.valueOf(json.getOrDefault("improvement", "已优化文本结构和表达。")),
            promptTokens,
            completionTokens,
            Duration.between(start, Instant.now()).toMillis()
        );
    }

    public ConnectionProbeResult testLlmConnection(AppConfigEntity currentConfig) {
        AppProperties.ModelEndpoint config = resolveLlmEndpoint(currentConfig);
        validate(config.getApiKey(), "LLM_API_KEY 未配置");
        Instant start = Instant.now();
        Map<String, Object> body = Map.of(
            "model", config.getModel(),
            "max_completion_tokens", 16,
            "messages", List.of(
                Map.of("role", "system", "content", "你是连接测试助手，只返回 OK。"),
                Map.of("role", "user", "content", "请返回 OK")
            )
        );

        Map<?, ?> response = client(config)
            .post()
            .uri(buildV1Uri(config, "/chat/completions"))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .onStatus(HttpStatusCode::isError, clientResponse -> mapModelError(clientResponse, "LLM 连接测试失败"))
            .bodyToMono(Map.class)
            .onErrorResume(ex -> Mono.error(wrapModelException(ex, "LLM 连接测试失败")))
            .retryWhen(retrySpec("LLM 连接测试失败"))
            .block(requestTimeout());

        String content = extractMessageText(firstMessage(response));
        return new ConnectionProbeResult(true, content.isBlank() ? "LLM 连接成功" : "LLM 连接成功：" + content, Duration.between(start, Instant.now()).toMillis());
    }

    public ConnectionProbeResult testAsrConnection(File file, AppConfigEntity currentConfig) {
        TranscriptionResult result = transcribe(file, currentConfig);
        String sample = result.text() == null || result.text().isBlank() ? "未返回可见文本" : result.text();
        if (sample.length() > 80) {
            sample = sample.substring(0, 80) + "...";
        }
        return new ConnectionProbeResult(true, "ASR 真实音频测试成功：" + sample, result.durationMs());
    }

    private TranscriptionResult transcribeByOpenAiAudioApi(File file, AppProperties.ModelEndpoint config, AppConfigEntity currentConfig) {
        Instant start = Instant.now();
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("model", config.getModel());
        builder.part("file", new FileSystemResource(file));
        String language = resolveTranscriptionLanguage(currentConfig);
        if (!language.isBlank()) {
            builder.part("language", language);
        }
        builder.part("prompt", buildRecognitionInstruction(currentConfig));

        Map<?, ?> response = client(config)
            .post()
            .uri(buildV1Uri(config, "/audio/transcriptions"))
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .bodyValue(builder.build())
            .retrieve()
            .onStatus(HttpStatusCode::isError, clientResponse -> mapModelError(clientResponse, "调用语音识别失败（openai-compatible）"))
            .bodyToMono(Map.class)
            .onErrorResume(ex -> Mono.error(wrapModelException(ex, "调用语音识别失败（openai-compatible）")))
            .retryWhen(retrySpec("调用语音识别失败"))
            .block(requestTimeout());

        String text = response != null && response.containsKey("text")
            ? String.valueOf(response.get("text"))
            : "";
        return new TranscriptionResult(TextSupport.normalizeWhitespace(text), Duration.between(start, Instant.now()).toMillis());
    }

    private TranscriptionResult transcribeByXiaomiAudioUnderstanding(File file, AppProperties.ModelEndpoint config, AppConfigEntity currentConfig) {
        try {
            return doTranscribeByXiaomiAudioUnderstanding(file, config, currentConfig);
        } catch (ApiException ex) {
            if (!isInvalidAudioFormat(ex)) {
                throw ex;
            }
            throw new ApiException(
                HttpStatus.BAD_GATEWAY,
                "音频格式不被 Xiaomi MiMo 当前接口兼容，请在上传前转换为标准 WAV/MP3/FLAC/OGG，或使用前端内置转码后再上传。"
            );
        }
    }

    private TranscriptionResult doTranscribeByXiaomiAudioUnderstanding(File file, AppProperties.ModelEndpoint config, AppConfigEntity currentConfig) {
        Instant start = Instant.now();
        String mimeType = detectAudioMimeType(file);
        String audioData = buildBase64AudioData(file, mimeType);

        Map<String, Object> body = Map.of(
            "model", config.getModel(),
            "messages", List.of(
                Map.of(
                    "role", "system",
                    "content", buildRecognitionSystemPrompt(currentConfig)
                ),
                Map.of(
                    "role", "user",
                    "content", List.of(
                        Map.of(
                            "type", "input_audio",
                            "input_audio", Map.of("data", audioData)
                        ),
                        Map.of(
                            "type", "text",
                            "text", buildRecognitionInstruction(currentConfig)
                        )
                    )
                )
            ),
            "max_completion_tokens", 2048
        );

        Map<?, ?> response = client(config)
            .post()
            .uri(buildV1Uri(config, "/chat/completions"))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .onStatus(HttpStatusCode::isError, clientResponse ->
                mapModelError(clientResponse, "调用 Xiaomi MiMo 音频识别失败（provider=" + normalizedProvider(config) + ", model=" + config.getModel() + "）"))
            .bodyToMono(Map.class)
            .onErrorResume(ex -> Mono.error(wrapModelException(
                ex,
                "调用 Xiaomi MiMo 音频识别失败（provider=" + normalizedProvider(config) + ", model=" + config.getModel() + "）"
            )))
            .retryWhen(retrySpec("调用 Xiaomi MiMo 音频识别失败"))
            .block(requestTimeout());

        String text = extractAudioTranscript(response);
        return new TranscriptionResult(TextSupport.normalizeWhitespace(text), Duration.between(start, Instant.now()).toMillis());
    }

    private WebClient client(AppProperties.ModelEndpoint config) {
        Duration timeout = timeout();
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) Math.min(timeout.toMillis(), 30_000))
            .secure(ssl -> ssl
                .sslContext(buildJdkSslContext())
                .handshakeTimeout(timeout.plusSeconds(20))
                .closeNotifyFlushTimeout(Duration.ofSeconds(5))
                .closeNotifyReadTimeout(Duration.ofSeconds(5)))
            .responseTimeout(timeout.plusSeconds(20));

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .baseUrl(config.getBaseUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
            .build();
    }

    private io.netty.handler.ssl.SslContext buildJdkSslContext() {
        try {
            return SslContextBuilder.forClient()
                .sslProvider(SslProvider.JDK)
                .build();
        } catch (SSLException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "初始化模型 TLS 客户端失败：" + ex.getMessage());
        }
    }

    private Mono<? extends Throwable> mapModelError(ClientResponse response, String actionLabel) {
        return response.bodyToMono(String.class)
            .defaultIfEmpty("")
            .map(body -> new ApiException(HttpStatus.BAD_GATEWAY, actionLabel + "：" + renderRemoteError(response.statusCode(), body)));
    }

    private ApiException wrapModelException(Throwable ex, String actionLabel) {
        if (ex instanceof ApiException apiException) {
            return apiException;
        }
        return new ApiException(HttpStatus.BAD_GATEWAY, actionLabel + "：" + ex.getMessage());
    }

    private String renderRemoteError(HttpStatusCode statusCode, String body) {
        String detail = extractRemoteError(body);
        if (!detail.isBlank()) {
            return statusCode.value() + " " + detail;
        }
        if (body != null && !body.isBlank()) {
            return statusCode.value() + " " + body;
        }
        return statusCode.toString();
    }

    private String extractRemoteError(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            Map<?, ?> payload = objectMapper.readValue(body, Map.class);
            Object error = payload.get("error");
            if (error instanceof Map<?, ?> errorMap) {
                String message = Objects.toString(errorMap.get("message"), "").trim();
                String param = Objects.toString(errorMap.get("param"), "").trim();
                if (!message.isBlank() && !param.isBlank()) {
                    return message + " (" + param + ")";
                }
                if (!message.isBlank()) {
                    return message;
                }
                if (!param.isBlank()) {
                    return param;
                }
            }
        } catch (Exception ignored) {
        }
        return body.trim();
    }

    private String buildV1Uri(AppProperties.ModelEndpoint config, String pathAfterV1) {
        String baseUrl = config.getBaseUrl() == null ? "" : config.getBaseUrl().trim();
        if (baseUrl.endsWith("/v1")) {
            return pathAfterV1;
        }
        if (baseUrl.endsWith("/v1/")) {
            return pathAfterV1.startsWith("/") ? pathAfterV1.substring(1) : pathAfterV1;
        }
        return "/v1" + pathAfterV1;
    }

    private Duration timeout() {
        return Duration.ofSeconds(appProperties.getModel().getTimeoutSeconds());
    }

    private Duration requestTimeout() {
        return timeout().plusSeconds(30);
    }

    private Retry retrySpec(String actionLabel) {
        return Retry.backoff(Math.max(1, appProperties.getModel().getMaxRetries()), Duration.ofSeconds(2))
            .maxBackoff(Duration.ofSeconds(8))
            .filter(this::isTransientModelFailure)
            .onRetryExhaustedThrow((spec, signal) -> {
                Throwable failure = signal.failure();
                return new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    actionLabel + "，已重试 " + signal.totalRetries() + " 次后仍失败：" + (failure == null ? "未知错误" : failure.getMessage())
                );
            });
    }

    private void validate(String key, String message) {
        if (key == null || key.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private Map<?, ?> firstMessage(Map<?, ?> response) {
        if (response == null) {
            return null;
        }
        List<?> choices = (List<?>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        Map<?, ?> choice = (Map<?, ?>) choices.get(0);
        return (Map<?, ?>) choice.get("message");
    }

    private String extractAudioTranscript(Map<?, ?> response) {
        Map<?, ?> message = firstMessage(response);
        String content = TextSupport.normalizeWhitespace(extractMessageText(message));
        if (!content.isBlank()) {
            return content;
        }
        Object reasoning = message == null ? null : message.get("reasoning_content");
        return reasoning == null ? "" : TextSupport.normalizeWhitespace(String.valueOf(reasoning));
    }

    private String extractMessageText(Map<?, ?> message) {
        if (message == null) {
            return "";
        }
        Object content = message.get("content");
        if (content == null) {
            return "";
        }
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof List<?> parts) {
            List<String> values = new ArrayList<>();
            for (Object part : parts) {
                if (part instanceof Map<?, ?> partMap) {
                    Object text = partMap.get("text");
                    if (text != null) {
                        values.add(String.valueOf(text));
                    }
                }
            }
            return String.join("\n", values);
        }
        return String.valueOf(content);
    }

    private String normalizedProvider(AppProperties.ModelEndpoint config) {
        return Objects.toString(config.getProvider(), "").trim().toLowerCase(Locale.ROOT);
    }

    private AppProperties.ModelEndpoint resolveAsrEndpoint(AppConfigEntity currentConfig) {
        return copyEndpoint(
            appProperties.getAsr(),
            safeConfigValue(currentConfig == null ? null : currentConfig.getAsrModelRoute(), appProperties.getAsr().getModel())
        );
    }

    private AppProperties.ModelEndpoint resolveLlmEndpoint(AppConfigEntity currentConfig) {
        return copyEndpoint(
            appProperties.getLlm(),
            safeConfigValue(currentConfig == null ? null : currentConfig.getLlmModelRoute(), appProperties.getLlm().getModel())
        );
    }

    private AppProperties.ModelEndpoint copyEndpoint(AppProperties.ModelEndpoint source, String modelOverride) {
        AppProperties.ModelEndpoint copy = new AppProperties.ModelEndpoint();
        copy.setProvider(source.getProvider());
        copy.setBaseUrl(source.getBaseUrl());
        copy.setApiKey(source.getApiKey());
        copy.setModel(modelOverride == null || modelOverride.isBlank() ? source.getModel() : modelOverride);
        return copy;
    }

    private boolean isXiaomiAudioUnderstanding(AppProperties.ModelEndpoint config) {
        String provider = normalizedProvider(config);
        String model = Objects.toString(config.getModel(), "").trim().toLowerCase(Locale.ROOT);
        String baseUrl = Objects.toString(config.getBaseUrl(), "").trim().toLowerCase(Locale.ROOT);
        return provider.contains("xiaomi")
            || provider.contains("mimo")
            || model.equals("mimo-v2.5")
            || model.equals("mimo-v2-omni")
            || baseUrl.contains("xiaomimimo.com");
    }

    private boolean isTransientModelFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConnectException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("failed to resolve")
                    || normalized.contains("handshake timed out")
                    || normalized.contains("ssl")
                    || normalized.contains("connection reset")
                    || normalized.contains("connection refused")
                    || normalized.contains("unexpected eof")
                    || normalized.contains("timeout")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isInvalidAudioFormat(ApiException ex) {
        String message = Objects.toString(ex.getMessage(), "").toLowerCase(Locale.ROOT);
        return message.contains("invalid audio format");
    }

    private String buildRecognitionSystemPrompt(AppConfigEntity currentConfig) {
        return """
            你是语音转写助手。
            任务目标：将输入音频准确转成可直接使用的文本。
            识别模型策略：%s
            语言策略：%s
            领域策略：%s
            稳定性策略：%s
            """.formatted(
            safeConfigValue(currentConfig == null ? null : currentConfig.getRecognitionModel(), "通用语音识别 v3"),
            safeConfigValue(currentConfig == null ? null : currentConfig.getLanguageType(), "中文（普通话）"),
            safeConfigValue(currentConfig == null ? null : currentConfig.getDomainModel(), "通用领域"),
            safeConfigValue(currentConfig == null ? null : currentConfig.getStabilityMode(), "平衡")
        ).trim();
    }

    private String buildRecognitionInstruction(AppConfigEntity currentConfig) {
        String recognitionModel = safeConfigValue(currentConfig == null ? null : currentConfig.getRecognitionModel(), "通用语音识别 v3");
        String languageType = safeConfigValue(currentConfig == null ? null : currentConfig.getLanguageType(), "中文（普通话）");
        String domainModel = safeConfigValue(currentConfig == null ? null : currentConfig.getDomainModel(), "通用领域");
        String stabilityMode = safeConfigValue(currentConfig == null ? null : currentConfig.getStabilityMode(), "平衡");

        String recognitionStyle = switch (recognitionModel) {
            case "快速语音识别 v3" -> "优先快速给出清晰可读的转写结果，避免过度猜测。";
            case "高精度语音识别 v3" -> "优先保留细节、术语、数字和易混淆片段，宁可完整也不要过度省略。";
            default -> "在准确率和速度之间保持均衡。";
        };
        String languageInstruction = switch (languageType) {
            case "中英混合" -> "允许中文与英文混合输出，英文术语、缩写、命令保持原样。";
            case "英文" -> "以英文转写为主，不要强行翻译成中文。";
            default -> "输出简体中文，英文术语、代码、命令和缩写保持原样。";
        };
        String domainInstruction = switch (domainModel) {
            case "会议场景" -> "注意识别会议里的决定、事项、人名和时间表达。";
            case "技术表达" -> "优先保留技术术语、英文变量名、接口名、缩写和数字。";
            case "客服沟通" -> "注意识别称呼、问题描述、订单信息和处理结论。";
            default -> "按通用表达转写，保证自然可读。";
        };
        String stabilityInstruction = switch (stabilityMode) {
            case "更快" -> "在不明显丢信息的前提下，优先快速完成转写。";
            case "更准确" -> "优先准确与完整，保留更多细节和上下文。";
            default -> "保持稳定和均衡。";
        };

        return """
            请将这段音频转写为可直接使用的文本，只输出转写结果本身，不要加标题、解释、说话人标签、总结或引号。
            %s
            %s
            %s
            %s
            """.formatted(recognitionStyle, languageInstruction, domainInstruction, stabilityInstruction).trim();
    }

    private String buildOptimizationSystemPrompt(AppConfigEntity currentConfig) {
        String optimizationModel = safeConfigValue(currentConfig == null ? null : currentConfig.getOptimizationModel(), "文本优化增强 v2");
        String costMode = safeConfigValue(currentConfig == null ? null : currentConfig.getCostMode(), "成本优先");

        String optimizationStrategy = switch (optimizationModel) {
            case "结构化整理 v2" -> "优先整理层次和结构，让结果更适合直接归档。";
            case "正式表达增强 v2" -> "优先提升正式度、严谨度和成文质量。";
            default -> "优先兼顾可读性、结构和表达流畅度。";
        };
        String costStrategy = switch (costMode) {
            case "质量优先" -> "优先输出更完整、更充分的内容，可以展开说明。";
            default -> "优先控制篇幅和成本，避免不必要的展开。";
        };

        return """
            你是中文文本整理助手。
            %s
            %s
            你必须遵守用户给出的场景、语气、长度和输出格式要求。
            """.formatted(optimizationStrategy, costStrategy).trim();
    }

    private int resolveOptimizationTokenBudget(AppConfigEntity currentConfig) {
        String lengthPreference = safeConfigValue(currentConfig == null ? null : currentConfig.getLengthPreference(), "适中");
        String costMode = safeConfigValue(currentConfig == null ? null : currentConfig.getCostMode(), "成本优先");
        int base = switch (lengthPreference) {
            case "精简" -> 900;
            case "详细" -> 1800;
            default -> 1300;
        };
        if ("质量优先".equals(costMode)) {
            return base + 400;
        }
        return base;
    }

    private String resolveTranscriptionLanguage(AppConfigEntity currentConfig) {
        String languageType = safeConfigValue(currentConfig == null ? null : currentConfig.getLanguageType(), "");
        return switch (languageType) {
            case "英文" -> "en";
            default -> "zh";
        };
    }

    private String safeConfigValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String buildBase64AudioData(File file, String mimeType) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String encoded = Base64.getEncoder().encodeToString(bytes);
            String data = "data:" + mimeType + ";base64," + encoded;
            if (data.length() > XIAOMI_AUDIO_BASE64_LIMIT) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "当前音频转为 Base64 后超过 50MB，无法使用 Xiaomi MiMo 音频理解模式，请缩短音频或更换 ASR 服务。");
            }
            return data;
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "读取音频文件失败：" + ex.getMessage());
        }
    }

    private String detectAudioMimeType(File file) {
        String fileName = file.getName().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".mp3")) {
            return "audio/mpeg";
        }
        if (fileName.endsWith(".wav")) {
            return "audio/wav";
        }
        if (fileName.endsWith(".flac")) {
            return "audio/flac";
        }
        if (fileName.endsWith(".m4a")) {
            return "audio/m4a";
        }
        if (fileName.endsWith(".ogg")) {
            return "audio/ogg";
        }
        try {
            String probed = Files.probeContentType(file.toPath());
            return probed == null || probed.isBlank() ? "audio/wav" : probed;
        } catch (IOException ex) {
            return "audio/wav";
        }
    }

    public record TranscriptionResult(String text, long durationMs) {
    }

    public record OptimizationResult(
        String title,
        String summary,
        String optimizedText,
        String markdown,
        String improvement,
        int promptTokens,
        int completionTokens,
        long durationMs
    ) {
    }

    public record ConnectionProbeResult(boolean success, String message, long durationMs) {
    }
}
