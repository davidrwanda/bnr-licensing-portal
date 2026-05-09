package rw.bnr.licensing.common.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * @author David NTAMAKEMWA
 *
 * Consistent pagination wrapper. Every paginated endpoint returns this shape so
 * clients never have to deal with Spring's internal Page serialisation format.
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
