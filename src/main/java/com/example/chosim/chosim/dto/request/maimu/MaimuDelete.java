package com.example.chosim.chosim.dto.request.maimu;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class MaimuDelete {

    @NotBlank(message = "please write title")
    private String title;

    @NotBlank(message = "please write message")
    private String message;

    @NotBlank(message = "please write maimuColor")
    private String maimuColor;

    @NotBlank(message = "please write writerName")
    private String writerName;

    @NotBlank(message = "please write sugarContent")
    private Integer sugarContent;

    public MaimuDelete(){
    }

    public MaimuDelete(String title, String message, String maimuColor, String writerName, Integer sugarContent) {
        this.title = title;
        this.message = message;
        this.maimuColor = maimuColor;
        this.writerName = writerName;
        this.sugarContent = sugarContent;
    }
}