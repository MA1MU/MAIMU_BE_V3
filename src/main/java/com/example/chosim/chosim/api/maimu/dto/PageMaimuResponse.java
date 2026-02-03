package com.example.chosim.chosim.api.maimu.dto;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter  // Lombok @Getter 추가
public class PageMaimuResponse<T> {
    private final String nickName;
    private final List<T> data;
    private final int currentPage;
    private final int totalPage;

    public PageMaimuResponse(String nickName, Page<T> page) {
        this.nickName = nickName;
        this.data = page.getContent();
        this.currentPage = page.getNumber();
        this.totalPage = page.getTotalPages();
    }
}
