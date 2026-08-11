package com.campuscart.college.service;

import com.campuscart.admin.service.AdminAccessService;
import com.campuscart.audit.service.AuditLogService;
import com.campuscart.college.domain.College;
import com.campuscart.college.dto.CollegeRequest;
import com.campuscart.college.dto.CollegeResponse;
import com.campuscart.college.repository.CollegeRepository;
import com.campuscart.common.api.PageResponse;
import com.campuscart.common.exception.BusinessRuleException;
import com.campuscart.common.exception.DuplicateResourceException;
import com.campuscart.common.exception.ResourceNotFoundException;
import com.campuscart.location.domain.City;
import com.campuscart.location.repository.CityRepository;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCollegeService {

    private final CollegeRepository collegeRepository;
    private final CityRepository cityRepository;
    private final AdminAccessService adminAccessService;
    private final AuditLogService auditLogService;

    public AdminCollegeService(CollegeRepository collegeRepository, CityRepository cityRepository,
                               AdminAccessService adminAccessService, AuditLogService auditLogService) {
        this.collegeRepository = collegeRepository;
        this.cityRepository = cityRepository;
        this.adminAccessService = adminAccessService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public PageResponse<CollegeResponse> list(UUID adminId, int page, int size) {
        adminAccessService.requireAdmin(adminId);
        return PageResponse.from(collegeRepository.findAllByOrderByNameAsc(pageRequest(page, size)).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public CollegeResponse get(UUID adminId, UUID collegeId) {
        adminAccessService.requireAdmin(adminId);
        return toResponse(require(collegeId));
    }

    @Transactional
    public CollegeResponse create(UUID adminId, CollegeRequest request) {
        var admin = adminAccessService.requireAdmin(adminId);
        City city = requireActiveCity(request.cityId());
        String name = request.name().trim();
        if (collegeRepository.existsByNameIgnoreCaseAndCityId(name, city.getId())) {
            throw new DuplicateResourceException("A college with that name already exists in the city.");
        }
        College college = collegeRepository.save(new College(name, city));
        auditLogService.record(admin, "COLLEGE_CREATED", "COLLEGE", college.getId(), "College created.");
        return toResponse(college);
    }

    @Transactional
    public CollegeResponse update(UUID adminId, UUID collegeId, CollegeRequest request) {
        var admin = adminAccessService.requireAdmin(adminId);
        College college = require(collegeId);
        City city = requireActiveCity(request.cityId());
        String name = request.name().trim();
        if (collegeRepository.existsByNameIgnoreCaseAndCityIdAndIdNot(name, city.getId(), collegeId)) {
            throw new DuplicateResourceException("A college with that name already exists in the city.");
        }
        college.update(name, city);
        auditLogService.record(admin, "COLLEGE_UPDATED", "COLLEGE", collegeId, "College updated.");
        return toResponse(college);
    }

    @Transactional
    public CollegeResponse activate(UUID adminId, UUID collegeId) {
        var admin = adminAccessService.requireAdmin(adminId);
        College college = require(collegeId);
        if (!college.getCity().isActive()) {
            throw new BusinessRuleException("A college cannot be activated in an inactive city.");
        }
        college.activate();
        auditLogService.record(admin, "COLLEGE_ACTIVATED", "COLLEGE", collegeId, "College activated.");
        return toResponse(college);
    }

    @Transactional
    public CollegeResponse deactivate(UUID adminId, UUID collegeId) {
        var admin = adminAccessService.requireAdmin(adminId);
        College college = require(collegeId);
        college.deactivate();
        auditLogService.record(admin, "COLLEGE_DEACTIVATED", "COLLEGE", collegeId, "College deactivated.");
        return toResponse(college);
    }

    private College require(UUID id) {
        return collegeRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("College", id));
    }

    private City requireActiveCity(UUID id) {
        return cityRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> ResourceNotFoundException.of("City", id));
    }

    private PageRequest pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new BusinessRuleException("Page must be non-negative and size must be between 1 and 50.");
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
    }

    private CollegeResponse toResponse(College college) {
        return new CollegeResponse(college.getId(), college.getName(), college.getCity().getId(),
                college.getCity().getName(), college.isActive(), college.getCreatedAt(), college.getUpdatedAt());
    }
}
