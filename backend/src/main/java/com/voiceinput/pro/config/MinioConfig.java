package com.voiceinput.pro.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(AppProperties appProperties) {
        return MinioClient.builder()
            .endpoint(appProperties.getStorage().getEndpoint())
            .credentials(
                appProperties.getStorage().getAccessKey(),
                appProperties.getStorage().getSecretKey()
            )
            .build();
    }
}

