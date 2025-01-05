package com.example.chosim.chosim.api.maimu.dto;

import com.example.chosim.chosim.api.group.dto.GroupResponse;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MaimuListResponse<T> {
    private GroupResponse groupResponse;
    private T data;
}
