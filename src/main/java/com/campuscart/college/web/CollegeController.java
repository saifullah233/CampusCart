package com.campuscart.college.web;

import com.campuscart.college.dto.CollegeDetectionResponse;
import com.campuscart.college.dto.CollegeResponse;
import com.campuscart.college.service.CollegeService;
import com.campuscart.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/colleges")
public class CollegeController {

    private final CollegeService collegeService;

    public CollegeController(CollegeService collegeService) {
        this.collegeService = collegeService;
    }

    @GetMapping
    public ApiResponse<List<CollegeResponse>> list(@RequestParam UUID cityId) {
        return ApiResponse.ok("Colleges retrieved.", collegeService.listActive(cityId));
    }

    @GetMapping("/by-email-domain/{domain}")
    public ApiResponse<CollegeDetectionResponse> getByEmailDomain(@PathVariable String domain) {
        CollegeDetectionResponse detected = collegeService.findByEmailDomain(domain);
        return ApiResponse.ok(detected != null ? "College detected." : "No college found for domain.", detected);
    }
}
