package com.retailco.productservice.dto;

import java.util.List;

// CONCEPT: Response DTO for a paginated list -- carries one "page" of
// results plus metadata (page number, size, total count/pages) so a
// caller knows how to fetch the next page.
public class ProductPageResponse {

    private List<ProductResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public ProductPageResponse() {
    }

    public ProductPageResponse(List<ProductResponse> content, int page, int size,
                                long totalElements, int totalPages) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public List<ProductResponse> getContent() { return content; }
    public void setContent(List<ProductResponse> content) { this.content = content; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}
