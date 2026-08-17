package com.campuscart.college.repository;

import com.campuscart.college.domain.CollegeEmailDomain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CollegeEmailDomainRepository extends JpaRepository<CollegeEmailDomain, UUID> {

    Optional<CollegeEmailDomain> findByDomain(String domain);

    boolean existsByCollegeId(UUID collegeId);

    List<CollegeEmailDomain> findAllByCollegeId(UUID collegeId);
}
