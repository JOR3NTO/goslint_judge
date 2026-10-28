package co.uceva.auth.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Página de resultados con los totales que necesita el paginador del frontend. */
public record PageResponse<T>(List<T> content, int page, int size, int totalPages, long totalElements) {

    public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                page.getTotalElements());
    }
}
