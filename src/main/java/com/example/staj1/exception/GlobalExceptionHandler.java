package com.example.staj1.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice // hata olduğunda JSON döndürecek.
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> error = new HashMap<>();

        String message = ex.getBindingResult()
                .getFieldErrors()
                .get(0)
                .getDefaultMessage();

        error.put("message", message);

        return ResponseEntity
                .badRequest()
                .body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleMessageNotReadable(
            HttpMessageNotReadableException ex) {

        Map<String, String> error = new HashMap<>();

        if (ex.getMessage() != null &&
                ex.getMessage().contains("Metin değeri girilmelidir.")) {

            error.put("message", "Metin değeri girilmelidir.");

        } else if (ex.getMessage() != null &&
                ex.getMessage().contains("Değer sayı olmalıdır.")) {

            error.put("message", "Değer sayı olmalıdır.");

        } else {

            error.put("message", "Geçersiz istek.");
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }
    public static class DuplicateResourceException extends RuntimeException {

        public DuplicateResourceException(String message) {
            super(message);
        }
    }
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateResource(
            DuplicateResourceException ex) {

        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEntityNotFound(
            EntityNotFoundException ex) {

        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }
    public static class PriceNotFoundException extends RuntimeException {

        public PriceNotFoundException(String message) {
            super(message);
        }
    }
    @ExceptionHandler(PriceNotFoundException.class)
    public ResponseEntity<String> handlePriceNotFound(PriceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(
            AccessDeniedException ex) {

        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(error);
    }

}