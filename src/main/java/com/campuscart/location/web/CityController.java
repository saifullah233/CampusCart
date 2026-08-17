package com.campuscart.location.web;

import com.campuscart.common.api.ApiResponse;
import com.campuscart.location.dto.CityResponse;
import com.campuscart.location.service.CityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cities")
public class CityController {

    private final CityService cityService;

    public CityController(CityService cityService) {
        this.cityService = cityService;
    }

    @GetMapping
    public ApiResponse<List<CityResponse>> list() {
        return ApiResponse.ok("Cities retrieved.", cityService.listActive());
    }
}
