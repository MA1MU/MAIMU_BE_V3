package com.example.chosim.chosim.api.maimu.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import static java.lang.Math.max;
import static java.lang.Math.min;

@Getter
@Setter
@Builder
public class MaimuSearch {

    private static final int MAX_SIZE = 2000;

    @Builder.Default
    private Integer page = 1;

    @Builder.Default
    private Integer size = 20;

    public long getOffset(){
        return (long) (max(1, page) - 1) * min(size, MAX_SIZE);
    }
}
