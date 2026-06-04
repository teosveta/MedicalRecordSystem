package com.medicalrecord.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 — ресурсът не е намерен
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .status(404)
                .error("Не е намерено")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(404).body(response);
    }

    // 403 — забранен достъп
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .status(403)
                .error("Забранен достъп")
                .message("Нямате права за достъп до този ресурс")
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(403).body(response);
    }

    // 401 — грешни данни за вход
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .status(401)
                .error("Грешни данни за вход")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(401).body(response);
    }

    // 400 — грешка в валидацията с детайли по полета
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", 400);
        response.put("грешка", "Невалидни данни");
        response.put("грешки", fieldErrors);
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(400).body(response);
    }

    // 409 — нарушение на ограничение за уникалност
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .status(409)
                .error("Конфликт на данни")
                .message("Записът нарушава ограничение за уникалност или целостност на данните")
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(409).body(response);
    }

    // 400 — грешки в бизнес логиката (напр. невалидна дата на болничен)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .status(400)
                .error("Невалидни данни")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(400).body(response);
    }

    // 500 — непредвидена грешка
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        ErrorResponse response = ErrorResponse.builder()
                .status(500)
                .error("Вътрешна грешка на сървъра")
                .message("Възникна неочаквана грешка. Моля, опитайте отново по-късно")
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(500).body(response);
    }
}
