package com.campuscart.college.service;

import com.campuscart.college.domain.College;
import com.campuscart.college.dto.CollegeDetectionResponse;
import com.campuscart.college.dto.CollegeResponse;
import com.campuscart.college.repository.CollegeEmailDomainRepository;
import com.campuscart.college.repository.CollegeRepository;
import com.campuscart.common.exception.ResourceNotFoundException;
import com.campuscart.location.repository.CityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CollegeService {

    private final CollegeRepository collegeRepository;
    private final CityRepository cityRepository;
    private final CollegeEmailDomainRepository domainRepository;

    public CollegeService(CollegeRepository collegeRepository,
                          CityRepository cityRepository,
                          CollegeEmailDomainRepository domainRepository) {
        this.collegeRepository = collegeRepository;
        this.cityRepository = cityRepository;
        this.domainRepository = domainRepository;
    }

    @Transactional(readOnly = true)
    public List<CollegeResponse> listActive(UUID cityId) {
        // Ensure city exists and is active, otherwise return 404
        cityRepository.findByIdAndActiveTrue(cityId)
                .orElseThrow(() -> ResourceNotFoundException.of("City", cityId));

        return collegeRepository.findAllByCityIdAndActiveTrueOrderByNameAsc(cityId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CollegeDetectionResponse findByEmailDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return null;
        }
        String normalized = domain.trim().toLowerCase();
        return domainRepository.findByDomain(normalized)
                .filter(d -> d.getCollege() != null && d.getCollege().isActive()
                        && d.getCollege().getCity() != null && d.getCollege().getCity().isActive())
                .map(d -> new CollegeDetectionResponse(
                        d.getCollege().getId(),
                        d.getCollege().getName(),
                        d.getCollege().getCity().getId(),
                        d.getCollege().getCity().getName()
                ))
                .orElse(null);
    }

    private CollegeResponse toResponse(College college) {
        return new CollegeResponse(
                college.getId(),
                college.getName(),
                college.getCity().getId(),
                college.getCity().getName(),
                college.isActive(),
                college.getCreatedAt(),
                college.getUpdatedAt()
        );
    }
}
