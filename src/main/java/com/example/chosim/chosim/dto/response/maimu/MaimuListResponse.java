package com.example.chosim.chosim.dto.response.maimu;

import com.example.chosim.chosim.dto.response.group.GroupResponse;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MaimuListResponse<T> {
    private GroupResponse groupResponse;
    private T data;
}
