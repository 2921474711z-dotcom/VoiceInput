package com.voiceinput.pro.config;

import java.math.BigDecimal;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String frontendUrl;
    private String baseUrl;
    private String queueName;
    private Storage storage = new Storage();
    private ModelEndpoint llm = new ModelEndpoint();
    private ModelEndpoint asr = new ModelEndpoint();
    private ModelConfig model = new ModelConfig();
    private Pricing pricing = new Pricing();

    @Data
    public static class Storage {
        private String bucket;
        private String endpoint;
        private String accessKey;
        private String secretKey;
    }

    @Data
    public static class ModelEndpoint {
        private String provider;
        private String baseUrl;
        private String apiKey;
        private String model;
    }

    @Data
    public static class ModelConfig {
        private Integer timeoutSeconds = 120;
        private Integer maxRetries = 2;
    }

    @Data
    public static class Pricing {
        private BigDecimal asrPricePerMinute = new BigDecimal("0.006");
        private BigDecimal llmInputPricePer1k = new BigDecimal("0.0008");
        private BigDecimal llmOutputPricePer1k = new BigDecimal("0.0032");
    }
}

