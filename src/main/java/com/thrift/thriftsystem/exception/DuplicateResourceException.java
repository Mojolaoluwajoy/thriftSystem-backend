package com.thrift.thriftsystem.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BaseException {

    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
