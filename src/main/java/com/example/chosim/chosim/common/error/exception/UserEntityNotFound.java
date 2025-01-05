package com.example.chosim.chosim.common.error.exception;

public class UserEntityNotFound extends MaimuException{

    private static final String MESSAGE = "CANNOT FIND USER";

    public UserEntityNotFound(){
        super(MESSAGE);
    }
    @Override
    public int getStatusCode() {
        return 404;
    }
}
