package com.campuscart.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring Boot EnvironmentPostProcessor that automatically loads environment variables
 * from the local `.env` file during local development (e.g. {@code mvn spring-boot:run}).
 *
 * <p>Preserves OS / shell environment variables with higher precedence so production
 * container environments continue to take priority.</p>
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(DotenvEnvironmentPostProcessor.class);
    private static final String PROPERTY_SOURCE_NAME = "dotenvProperties";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> dotenvMap = loadDotenvMap();
        if (!dotenvMap.isEmpty()) {
            if (environment.getPropertySources().contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
                environment.getPropertySources().addAfter(
                        StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                        new MapPropertySource(PROPERTY_SOURCE_NAME, dotenvMap));
            } else {
                environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, dotenvMap));
            }
            log.info("Loaded {} environment properties from .env file into Spring Environment", dotenvMap.size());
        }
    }

    /**
     * Finds and parses .env file from working directory or user directory.
     */
    public static Map<String, Object> loadDotenvMap() {
        Map<String, Object> properties = new LinkedHashMap<>();
        Path dotenvPath = findDotenvPath();
        if (dotenvPath == null || !Files.isRegularFile(dotenvPath)) {
            return properties;
        }

        try (BufferedReader reader = Files.newBufferedReader(dotenvPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("export ")) {
                    line = line.substring(7).trim();
                }
                int equalsIdx = line.indexOf('=');
                if (equalsIdx <= 0) {
                    continue;
                }

                String key = line.substring(0, equalsIdx).trim();
                String rawValue = line.substring(equalsIdx + 1).trim();

                // Unquote if wrapped in single or double quotes
                String value = unquote(rawValue);
                if (!key.isEmpty()) {
                    properties.put(key, value);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to read .env file at {}: {}", dotenvPath, e.getMessage());
        }
        return properties;
    }

    private static Path findDotenvPath() {
        // 1. Current working directory
        Path pwd = Paths.get(".").toAbsolutePath().normalize();
        Path candidate = pwd.resolve(".env");
        if (Files.isRegularFile(candidate)) {
            return candidate;
        }

        // 2. User directory system property
        String userDir = System.getProperty("user.dir");
        if (userDir != null) {
            candidate = Paths.get(userDir).resolve(".env");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }

        // 3. Parent directory (e.g. if run from a submodule)
        if (pwd.getParent() != null) {
            candidate = pwd.getParent().resolve(".env");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private static String unquote(String val) {
        if (val == null) {
            return "";
        }
        val = val.trim();
        if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
            if (val.length() >= 2) {
                return val.substring(1, val.length() - 1);
            }
        }
        return val;
    }
}
