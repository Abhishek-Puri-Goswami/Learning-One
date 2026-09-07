package com.retailco.orderservice.exception;

import com.retailco.orderservice.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

// CONCEPT: Global exception handling -- converts exceptions from any
// controller into a consistent JSON error response. Each new exception
// type added over time (OutOfStock, InvalidCoupon) gets its own handler
// mapped to the most fitting HTTP status.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                            HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest().body(new ErrorResponse(
                Instant.now(), HttpStatus.BAD_REQUEST.value(), "Bad Request",
                "Validation failed for one or more fields", request.getRequestURI(), details));
    }

    @ExceptionHandler(EmptyCartException.class)
    public ResponseEntity<ErrorResponse> handleEmptyCart(EmptyCartException ex, HttpServletRequest request) {
        return badRequest(ex.getMessage(), request);
    }

    // L1/UC5 addition: OutOfStockException -> 409 Conflict (the cart's
    // contents conflict with current catalog state), distinct from a plain
    // validation error.
    @ExceptionHandler(OutOfStockException.class)
    public ResponseEntity<ErrorResponse> handleOutOfStock(OutOfStockException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                Instant.now(), HttpStatus.CONFLICT.value(), "Conflict",
                ex.getMessage(), request.getRequestURI(), List.of()));
    }

    // L1/UC5 addition: InvalidCouponException -> 400.
    @ExceptionHandler(InvalidCouponException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCoupon(InvalidCouponException ex, HttpServletRequest request) {
        return badRequest(ex.getMessage(), request);
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentFailed(PaymentFailedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(new ErrorResponse(
                Instant.now(), HttpStatus.PAYMENT_REQUIRED.value(), "Payment Required",
                ex.getMessage(), request.getRequestURI(), List.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        return ResponseEntity.internalServerError().body(new ErrorResponse(
                Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error",
                "An unexpected error occurred", request.getRequestURI(), List.of()));
    }

    private ResponseEntity<ErrorResponse> badRequest(String message, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                Instant.now(), HttpStatus.BAD_REQUEST.value(), "Bad Request",
                message, request.getRequestURI(), List.of()));
    }
}
