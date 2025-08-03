package com.railse.hiring.workforcemgmt.common.exception;

import com.railse.hiring.workforcemgmt.common.model.response.Response;
import com.railse.hiring.workforcemgmt.common.model.response.ResponseStatus;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Response<Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ResponseStatus status = new ResponseStatus(StatusCode.NOT_FOUND.getCode(), ex.getMessage());
        return new ResponseEntity<>(new Response<>(null, null, status), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            validationErrors.put(error.getField(), error.getDefaultMessage())
        );
        ResponseStatus status = new ResponseStatus(StatusCode.BAD_REQUEST.getCode(), "Validation failed");
        return new ResponseEntity<>(new Response<>(validationErrors, null, status), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Response<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> validationErrors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String field = violation.getPropertyPath().toString();
            validationErrors.put(field, violation.getMessage());
        });
        ResponseStatus status = new ResponseStatus(StatusCode.BAD_REQUEST.getCode(), "Validation failed");
        return new ResponseEntity<>(new Response<>(validationErrors, null, status), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<Object>> handleAllExceptions(Exception ex) {
        ResponseStatus status = new ResponseStatus(
            StatusCode.INTERNAL_SERVER_ERROR.getCode(),
            "An unexpected error occurred: " + ex.getMessage()
        );
        return new ResponseEntity<>(new Response<>(null, null, status), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
