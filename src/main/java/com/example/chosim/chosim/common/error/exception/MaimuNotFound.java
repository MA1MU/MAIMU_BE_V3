package com.example.chosim.chosim.common.error.exception;

public class MaimuNotFound extends MaimuException{

    private static final String MESSAGE = "CANNOT FIND MAIMU";

    public MaimuNotFound(){
        super(MESSAGE);
    }

    @Override
    public int getStatusCode() {
        return 404;
    }
}
