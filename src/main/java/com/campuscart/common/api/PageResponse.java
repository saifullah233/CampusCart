package com.campuscart.common.api;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Serializable pagination envelope returned for paged collection endpoints.
 *
 * <p>Decouples the API contract from Spring Data's {@link Page} (which serializes an
 * unstable, internal shape). Controllers map a {@code Page<Entity>} to a
 * {@code PageResponse<Dto>} and wrap it in {@link ApiResponse}.</p>
 *
 * @param <T> the element (DTO) type
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    /**
     * Builds a {@code PageResponse} from an already-mapped Spring Data {@link Page}.
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
