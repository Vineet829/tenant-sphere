package com.tenantsphere.common;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public record PageResponse<T>(long count, String next, String previous, List<T> results, int pageSize, int totalPages) {

    public static String currentRequestUrl() {
        return ServletUriComponentsBuilder.fromCurrentRequest().replaceQueryParam("page")
                .build().toUriString();
    }

    public static <T> PageResponse<T> from(Page<T> page, String baseUrl) {
        String next = page.hasNext() ? pageUrl(baseUrl, page.getNumber() + 2) : null;
        String previous = page.hasPrevious() ? pageUrl(baseUrl, page.getNumber()) : null;
        return new PageResponse<>(page.getTotalElements(), next, previous, page.getContent(), page.getSize(), page.getTotalPages());
    }

    private static String pageUrl(String baseUrl, int pageNumber) {
        if (pageNumber <= 1) {
            return baseUrl;
        }
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + "page=" + pageNumber;
    }
}
