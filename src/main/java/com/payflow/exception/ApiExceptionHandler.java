package com.payflow.exception;

import com.payflow.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> notFound(RuntimeException e, HttpServletRequest req) { return error(HttpStatus.NOT_FOUND, e.getMessage(), req, Map.of()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalid(MethodArgumentNotValidException e, HttpServletRequest req) {
        Map<String,String> details = e.getBindingResult().getFieldErrors().stream().collect(Collectors.toMap(
                f -> f.getField(), f -> f.getDefaultMessage(), (a,b) -> a, LinkedHashMap::new));
        return error(HttpStatus.BAD_REQUEST, "Request validation failed", req, details);
    }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, org.springframework.http.converter.HttpMessageNotReadableException.class})
    ResponseEntity<ApiError> malformed(Exception e, HttpServletRequest req) { return error(HttpStatus.BAD_REQUEST, "Invalid request parameter or body", req, Map.of()); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> internal(Exception e, HttpServletRequest req) { return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", req, Map.of()); }
    private ResponseEntity<ApiError> error(HttpStatus status, String message, HttpServletRequest req, Map<String,String> details) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, req.getRequestURI(), details));
    }
}
