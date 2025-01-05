package com.example.chosim.chosim.api.maimu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
public class MaimuCreate {

    @NotBlank(message = "please write title")
    private String title;

    @NotBlank(message = "please write message")
    private String message;

    @NotBlank(message = "please write maimuColor")
    private String maimuColor;

    @NotBlank(message = "please write writerName")
    private String writerName;

    @NotNull(message = "please write sugarContent")
    private Integer sugarContent;

    @Builder
    public MaimuCreate(String title, String message, String maimuColor, String writerName, Integer sugarContent){
        this.title = title;
        this.message = message;
        this.maimuColor = maimuColor;
        this.writerName = writerName;
        this.sugarContent = sugarContent;
    }

}
