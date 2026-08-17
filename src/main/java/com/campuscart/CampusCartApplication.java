package com.campuscart;

import com.campuscart.config.DotenvEnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

/**
 * CampusCart backend entry point.
 *
 * <p>Everything on Campus. By Students. For Students.</p>
 */
@SpringBootApplication
public class CampusCartApplication {

    public static void main(String[] args) {
        Map<String, Object> dotenv = DotenvEnvironmentPostProcessor.loadDotenvMap();
        dotenv.forEach((key, val) -> {
            if (System.getProperty(key) == null && System.getenv(key) == null && val != null) {
                System.setProperty(key, val.toString());
            }
        });
        SpringApplication.run(CampusCartApplication.class, args);
    }
}
