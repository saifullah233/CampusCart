package com.campuscart.college.repository;

import com.campuscart.college.domain.College;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CollegeRepository extends JpaRepository<College, UUID> {

    Optional<College> findByIdAndCityId(UUID id, UUID cityId);

    boolean existsByNameIgnoreCaseAndCityId(String name, UUID cityId);

    boolean existsByNameIgnoreCaseAndCityIdAndIdNot(String name, UUID cityId, UUID id);

    Page<College> findAllByOrderByNameAsc(Pageable pageable);

    List<College> findAllByCityIdAndActiveTrueOrderByNameAsc(UUID cityId);
}

