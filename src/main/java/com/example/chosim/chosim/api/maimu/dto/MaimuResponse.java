package com.example.chosim.chosim.api.maimu.dto;

import com.example.chosim.chosim.domain.maimu.entity.Maimu;
import lombok.Builder;
import lombok.Getter;

@Getter
public class MaimuResponse {

    private final Long id;
    private final String title;
    private final String message;
    private final String maimuColor;
    private final String writerName;
    private final Integer sugarContent;



    public MaimuResponse(Maimu maimu){
        this.id = maimu.getId();
        this.title = maimu.getTitle();
        this.message = maimu.getMessage();
        this.maimuColor = maimu.getMaimuColor();
        this.writerName = maimu.getWriterName();
        this.sugarContent = maimu.getSugarContent();
    }

    @Builder
    public MaimuResponse(Long id, String title, String message, String maimuColor, String writerName, Integer sugarContent){
        this.id = id;
        this.title = title;
        this.message = message;
        this.maimuColor = maimuColor;
        this.writerName = writerName;
        this.sugarContent = sugarContent;
    }
}
