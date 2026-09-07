package com.retailco.bankrag.service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

// CONCEPT: Centralized exception handling via `@RestControllerAdvice`
// (Spring's "global @ExceptionHandler" mechanism, applied across every
// @RestController in this application).
// PURPOSE: Without this class, an unhandled exception thrown from a
// controller method (e.g. IllegalArgumentException from a bad request)
// would produce Spring's default, generic error page/JSON -- inconsistent
// and unhelpful for API clients. This class intercepts specific exception
// types and turns each into a clean, structured JSON error body with a
// consistent shape (timestamp, status, error, message/fieldErrors).
// HOW IT WORKS: each `@ExceptionHandler(SomeException.class)` method is
// automatically invoked by Spring whenever ANY controller in the
// application throws that exception type -- no try/catch needed in the
// controllers themselves. This keeps error-formatting logic in ONE place
// instead of duplicated across every controller method.
// WHAT IF REMOVED: every controller would need its own try/catch around
// every risky call, and error response shapes would likely drift
// inconsistent across endpoints over time.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Failed");
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(UncheckedIOException.class)
    public ResponseEntity<Map<String, Object>> handleIo(UncheckedIOException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Corpus Load Failed");
        body.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Invalid Request");
        body.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(body);
    }
}
