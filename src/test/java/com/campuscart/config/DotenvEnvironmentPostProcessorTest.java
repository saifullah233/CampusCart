package com.campuscart.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DotenvEnvironmentPostProcessorTest {

    @Test
    void loadsDotenvPropertiesIntoEnvironment() {
        DotenvEnvironmentPostProcessor processor = new DotenvEnvironmentPostProcessor();
        ConfigurableEnvironment environment = new StandardEnvironment();
        processor.postProcessEnvironment(environment, new SpringApplication(Object.class));

        Map<String, Object> map = DotenvEnvironmentPostProcessor.loadDotenvMap();
        if (!map.isEmpty()) {
            assertThat(environment.getPropertySources().contains("dotenvProperties")).isTrue();
            for (String key : map.keySet()) {
                assertThat(environment.getProperty(key)).isNotNull();
            }
        }
    }
}
