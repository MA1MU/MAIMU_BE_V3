package com.example.chosim.chosim.exception;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public abstract class MaimuException extends RuntimeException{

    public final Map<String, String> validation = new HashMap<>();

    public MaimuException(String message) {
        super(message);
    }

    public MaimuException(String message, Throwable cause){
        super(message);
    }

    public abstract int getStatusCode();

    public void addValidation(String fieldName, String message){
        validation.put(fieldName, message);
    }
}

