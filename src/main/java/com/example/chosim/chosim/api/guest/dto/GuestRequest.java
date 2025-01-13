package com.example.chosim.chosim.api.guest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GuestRequest {

    @NotBlank(message = "제목을 작성해 주세요")
    private String title;

    @NotBlank(message = "내용을 작성해 주세요")
    private String message;

    @NotBlank(message = "색깔을 지정해 주세요")
    private String maimuColor;

    @NotBlank(message = "작성자 이름을 지정해 주세요")
    private String writerName;

    @NotNull(message = "당도를 지정해 주세요")
    private Integer sugarContent;

    @Builder
    public GuestRequest(String title, String message, String maimuColor, String writerName, Integer sugarContent){
        this.title = title;
        this.message = message;
        this.maimuColor = maimuColor;
        this.writerName = writerName;
        this.sugarContent = sugarContent;
    }

}
