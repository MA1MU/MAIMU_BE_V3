package com.example.chosim.chosim.dto.response.group;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GroupListResponse<T> {
    private T data;
}
