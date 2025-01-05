package com.example.chosim.chosim.api.group.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GroupListResponse<T> {
    private T data;
}
