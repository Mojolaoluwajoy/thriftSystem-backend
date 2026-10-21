package com.thrift.thriftsystem.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BaseException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
