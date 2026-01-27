package com.example.chosim.chosim.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResponseDTO<T>{
    String maimuProfile;
    private T data;
}
