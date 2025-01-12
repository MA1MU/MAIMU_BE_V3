package com.example.chosim.chosim.api.maimu.dto;

import com.example.chosim.chosim.domain.maimu.entity.Maimu;
import lombok.Builder;
import lombok.Getter;

@Getter
public class MaimuResponse {

    private final String title;
    private final String message;
    private final String maimuColor;
    private final String writerName;
    private final Integer sugarContent;
    private final boolean isAnonymous;

    public MaimuResponse(Maimu maimu){
        this.title = maimu.getTitle();
        this.message = maimu.getMessage();
        this.maimuColor = maimu.getMaimuColor().toString();
        this.writerName = maimu.getWriterName();
        this.sugarContent = maimu.getSugarContent();
        this.isAnonymous = maimu.isAnonymous();
    }

    @Builder
    public MaimuResponse(String title, String message, String maimuColor,
                         String writerName, Integer sugarContent, boolean isAnonymous) {
        this.title = title;
        this.message = message;
        this.maimuColor = maimuColor;
        this.writerName = writerName;
        this.sugarContent = sugarContent;
        this.isAnonymous = isAnonymous;
    }
}
