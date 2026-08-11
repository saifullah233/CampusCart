package com.campuscart.location.service;

import com.campuscart.admin.service.AdminAccessService;
import com.campuscart.audit.service.AuditLogService;
import com.campuscart.common.api.PageResponse;
import com.campuscart.common.exception.BusinessRuleException;
import com.campuscart.common.exception.DuplicateResourceException;
import com.campuscart.common.exception.ResourceNotFoundException;
import com.campuscart.location.domain.City;
import com.campuscart.location.dto.CityRequest;
import com.campuscart.location.dto.CityResponse;
import com.campuscart.location.repository.CityRepository;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCityService {

    private final CityRepository cityRepository;
    private final AdminAccessService adminAccessService;
    private final AuditLogService auditLogService;

    public AdminCityService(CityRepository cityRepository, AdminAccessService adminAccessService,
                            AuditLogService auditLogService) {
        this.cityRepository = cityRepository;
        this.adminAccessService = adminAccessService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public PageResponse<CityResponse> list(UUID adminId, int page, int size) {
        adminAccessService.requireAdmin(adminId);
        return PageResponse.from(cityRepository.findAllByOrderByNameAsc(pageRequest(page, size)).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public CityResponse get(UUID adminId, UUID cityId) {
        adminAccessService.requireAdmin(adminId);
        return toResponse(require(cityId));
    }

    @Transactional
    public CityResponse create(UUID adminId, CityRequest request) {
        var admin = adminAccessService.requireAdmin(adminId);
        String name = request.name().trim();
        String state = request.state().trim();
        if (cityRepository.existsByNameIgnoreCaseAndStateIgnoreCase(name, state)) {
            throw new DuplicateResourceException("A city with that name and state already exists.");
        }
        City city = cityRepository.save(new City(name, state));
        auditLogService.record(admin, "CITY_CREATED", "CITY", city.getId(), "City created.");
        return toResponse(city);
    }

    @Transactional
    public CityResponse update(UUID adminId, UUID cityId, CityRequest request) {
        var admin = adminAccessService.requireAdmin(adminId);
        City city = require(cityId);
        String name = request.name().trim();
        String state = request.state().trim();
        if (cityRepository.existsByNameIgnoreCaseAndStateIgnoreCaseAndIdNot(name, state, cityId)) {
            throw new DuplicateResourceException("A city with that name and state already exists.");
        }
        city.update(name, state);
        auditLogService.record(admin, "CITY_UPDATED", "CITY", cityId, "City updated.");
        return toResponse(city);
    }

    @Transactional
    public CityResponse activate(UUID adminId, UUID cityId) {
        var admin = adminAccessService.requireAdmin(adminId);
        City city = require(cityId);
        city.activate();
        auditLogService.record(admin, "CITY_ACTIVATED", "CITY", cityId, "City activated.");
        return toResponse(city);
    }

    @Transactional
    public CityResponse deactivate(UUID adminId, UUID cityId) {
        var admin = adminAccessService.requireAdmin(adminId);
        City city = require(cityId);
        city.deactivate();
        auditLogService.record(admin, "CITY_DEACTIVATED", "CITY", cityId, "City deactivated.");
        return toResponse(city);
    }

    private City require(UUID id) {
        return cityRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("City", id));
    }

    private PageRequest pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new BusinessRuleException("Page must be non-negative and size must be between 1 and 50.");
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
    }

    private CityResponse toResponse(City city) {
        return new CityResponse(city.getId(), city.getName(), city.getState(), city.isActive(),
                city.getCreatedAt(), city.getUpdatedAt());
    }
}
