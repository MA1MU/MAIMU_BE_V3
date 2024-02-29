package com.example.chosim.chosim.dto.response.maimu;

import com.example.chosim.chosim.domain.Maimu;
import lombok.Builder;
import lombok.Getter;

@Getter
public class MaimuResponseDto {
    private final Long id;

    public MaimuResponseDto(Maimu maimu){
        this.id = maimu.getId();
    }

    @Builder
    public MaimuResponseDto(Long id) {
        this.id = id;
    }
}
