package com.retailco.productservice.dto;

import java.util.List;

/**
 * Instead of sending back EVERY product at once (which could be
 * thousands), we send one "page" of results at a time, along with some
 * extra info: which page this is, how big a page is, and how many pages
 * there are in total. This lets the caller (like a web page showing a
 * product list) know how to ask for the next page.
 */
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
