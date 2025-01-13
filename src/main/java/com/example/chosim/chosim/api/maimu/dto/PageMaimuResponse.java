package com.example.chosim.chosim.api.maimu.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public class PageMaimuResponse <T>{
    List<T> data;
    int currentPage;
    int totalPage;

    public PageMaimuResponse(Page<T> page) {
        this.data = page.getContent();
        this.currentPage = page.getNumber();
        this.totalPage = page.getTotalPages();
    }
}
