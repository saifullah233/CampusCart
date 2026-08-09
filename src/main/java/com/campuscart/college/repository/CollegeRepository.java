package com.campuscart.college.repository;

import com.campuscart.college.domain.College;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CollegeRepository extends JpaRepository<College, UUID> {

    Optional<College> findByIdAndCityId(UUID id, UUID cityId);
}
