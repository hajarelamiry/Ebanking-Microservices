package com.example.demo.exception;

import com.example.demo.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire global des exceptions pour le crypto-service
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex) {
        log.warn("Insufficient balance: {}", ex.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        details.put("required", ex.getRequired());
        details.put("available", ex.getAvailable());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("Insufficient Balance")
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .service("crypto-service")
                .timestamp(LocalDateTime.now())
                .details(details)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("User Not Found")
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .service("crypto-service")
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex) {
        log.error("Service unavailable: {} - {}", ex.getServiceName(), ex.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        details.put("service", ex.getServiceName());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("Service Unavailable")
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .service("crypto-service")
                .timestamp(LocalDateTime.now())
                .details(details)
                .build();
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("Business Error")
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .service("crypto-service")
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("Invalid Request")
                .errorCode("INVALID_ARGUMENT")
                .message(ex.getMessage())
                .service("crypto-service")
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("Invalid State")
                .errorCode("INVALID_STATE")
                .message(ex.getMessage())
                .service("crypto-service")
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("Forbidden")
                .errorCode("ACCESS_DENIED")
                .message(ex.getMessage())
                .service("crypto-service")
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });
        
        ErrorResponse error = ErrorResponse.builder()
                .error("Validation Failed")
                .errorCode("VALIDATION_ERROR")
                .message("Request validation failed")
                .service("crypto-service")
                .timestamp(LocalDateTime.now())
                .details(validationErrors)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .error("Internal Server Error")
                .errorCode("INTERNAL_ERROR")
                .message("An unexpected error occurred")
                .service("crypto-service")
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
