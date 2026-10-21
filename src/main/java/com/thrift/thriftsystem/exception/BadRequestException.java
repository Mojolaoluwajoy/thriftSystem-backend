package com.thrift.thriftsystem.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends BaseException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
