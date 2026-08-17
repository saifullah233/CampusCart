package com.campuscart.location.service;

import com.campuscart.location.domain.City;
import com.campuscart.location.dto.CityResponse;
import com.campuscart.location.repository.CityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CityService {

    private final CityRepository cityRepository;

    public CityService(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    @Transactional(readOnly = true)
    public List<CityResponse> listActive() {
        return cityRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private CityResponse toResponse(City city) {
        return new CityResponse(
                city.getId(),
                city.getName(),
                city.getState(),
                city.isActive(),
                city.getCreatedAt(),
                city.getUpdatedAt()
        );
    }
}
