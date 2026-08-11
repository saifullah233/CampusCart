package com.campuscart.location.repository;

import com.campuscart.location.domain.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CityRepository extends JpaRepository<City, UUID> {

    Optional<City> findByIdAndActiveTrue(UUID id);

    boolean existsByNameIgnoreCaseAndStateIgnoreCase(String name, String state);

    boolean existsByNameIgnoreCaseAndStateIgnoreCaseAndIdNot(String name, String state, UUID id);

    Page<City> findAllByOrderByNameAsc(Pageable pageable);
}
