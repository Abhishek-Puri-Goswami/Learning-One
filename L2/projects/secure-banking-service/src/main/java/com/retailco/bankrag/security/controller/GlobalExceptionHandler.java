package com.retailco.bankrag.security.controller;

import com.retailco.bankrag.security.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Same @RestControllerAdvice pattern used across every Spring Boot service
 * in this submission. UnauthorizedException (raised by BankingToolService
 * for BOTH authentication and authorization failures -- see its Javadoc)
 * maps to a single 401 response shape here, deliberately not distinguishing
 * "bad token" from "wrong customer" in the HTTP response, to avoid leaking
 * which failure mode occurred.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", "Unauthorized");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }
}
