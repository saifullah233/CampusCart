package com.campuscart.common.web;

import com.campuscart.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Minimal liveness endpoint used to verify request wiring end-to-end.
 *
 * <p>Operational health/readiness is served by Spring Boot Actuator; this endpoint
 * exists so the application's own controller/serialization stack is exercised.</p>
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "System", description = "Service liveness and metadata")
public class HealthController {

    @Operation(summary = "Ping", description = "Returns a static liveness payload.")
    @GetMapping("/ping")
    public ApiResponse<PingResponse> ping() {
        return ApiResponse.ok(new PingResponse("campuscart-backend", "UP", Instant.now()));
    }

    public record PingResponse(String service, String status, Instant timestamp) {
    }
}
