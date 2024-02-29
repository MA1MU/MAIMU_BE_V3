package com.example.chosim.chosim.dto.response.maimu;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MaimuListResponse<T> {
    private T data;
}
