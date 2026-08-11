package com.campuscart.catalog.repository;

import com.campuscart.catalog.domain.Category;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    boolean existsBySlugIgnoreCaseAndIdNot(String slug, UUID id);

    Optional<Category> findBySlug(String slug);

    Optional<Category> findByIdAndActiveTrue(UUID id);

    List<Category> findByActiveTrueOrderByNameAsc();

    Page<Category> findAllByOrderByNameAsc(Pageable pageable);
}
