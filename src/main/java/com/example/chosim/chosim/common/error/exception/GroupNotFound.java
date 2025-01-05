package com.example.chosim.chosim.common.error.exception;

public class GroupNotFound extends MaimuException {

    private static final String MESSAGE = "CANNOT FIND GROUP";

    public GroupNotFound(){
        super(MESSAGE);
    }

    @Override
    public int getStatusCode() {
        return 404;
    }
}