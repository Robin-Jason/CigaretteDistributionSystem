package org.example.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 提供最简 MeterRegistry，便于本地/测试环境运行。
 * 如需接入 Prometheus/其他监控，再提供对应的 Registry Bean。
 */
@Configuration
public class MeterRegistryConfig {

    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}


