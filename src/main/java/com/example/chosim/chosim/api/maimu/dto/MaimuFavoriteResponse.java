package com.example.chosim.chosim.api.maimu.dto;

import lombok.Getter;

@Getter
public class MaimuFavoriteResponse {

    private final boolean isFavorite;

    public MaimuFavoriteResponse(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }
}
