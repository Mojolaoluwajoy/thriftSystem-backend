package com.thrift.thriftsystem.exception;

import com.thrift.thriftsystem.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse> handleBaseException(BaseException e) {
        log.error("Base Exception : {}",e.getMessage());
       return new ResponseEntity<>(ApiResponse.error(e.getMessage()),e.getStatus());

        }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.error("Access denied : {}",e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied"));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGenericException(Exception e) {
        log.error("Unexpected error: {}",e.getMessage());
        return new ResponseEntity<>(ApiResponse.error("An unexpected error occurred"),HttpStatus.INTERNAL_SERVER_ERROR);

    }
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse> handleBadCredentialsExceptions(BadCredentialsException e) {
        log.error("Bad credentials: {}",e.getMessage());
        return new ResponseEntity<>(ApiResponse.error("Invalid email or password"),HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error-> {
            String fieldName=((FieldError)error).getField();
            String errrorMessage=error.getDefaultMessage();
            errors.put(fieldName,errrorMessage);
        });
        return new ResponseEntity<>(ApiResponse.builder()
                .success(false)
                .message("Validation failed")
                .data(errors)
                .build(), HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgument(
            IllegalArgumentException ex) {
        log.error("Illegal argument: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(
                ApiResponse.error("Invalid request: " + ex.getMessage()),
                HttpStatus.BAD_REQUEST);
    }


}