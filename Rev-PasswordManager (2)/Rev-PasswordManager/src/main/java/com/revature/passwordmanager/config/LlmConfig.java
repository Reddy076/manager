package com.revature.passwordmanager.config;

import lombok.Data;
import okhttp3.OkHttpClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "llm")
@Data
public class LlmConfig {
    private String baseUrl;
    private String model;
    private double temperature = 0.7;
    private int timeoutSeconds = 60;

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .readTimeout(Duration.ofSeconds(timeoutSeconds))
                .writeTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }
}
