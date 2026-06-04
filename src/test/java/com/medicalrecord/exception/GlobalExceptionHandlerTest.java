package com.medicalrecord.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    // Директно инстанцираме handler-а — не е нужен Spring контекст
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // ResourceNotFoundException трябва да връща HTTP 404
    @Test
    void handleResourceNotFound_returns404Status() {
        ResourceNotFoundException ex =
                new ResourceNotFoundException("Пациент", "id", 5L);

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex);

        assertEquals(404, response.getStatusCode().value(),
                "HTTP статусът трябва да е 404");
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus(),
                "Полето status в тялото трябва да е 404");
        assertTrue(response.getBody().getMessage().contains("Пациент"),
                "Съобщението трябва да съдържа името на ресурса");
    }

    // ResourceNotFoundException трябва да включва id-то в съобщението
    @Test
    void handleResourceNotFound_includesResourceIdInMessage() {
        ResourceNotFoundException ex =
                new ResourceNotFoundException("Лекар", "id", 42L);

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex);

        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("42"),
                "Съобщението трябва да съдържа id-то на ресурса (42)");
    }

    // MethodArgumentNotValidException трябва да връща HTTP 400 с детайли по полета
    @Test
    void handleValidation_returns400WithFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError(
                "patientRequest", "egn",
                "ЕГН-то трябва да съдържа точно 10 цифри");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertEquals(400, response.getStatusCode().value(),
                "HTTP статусът трябва да е 400");
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("грешки"),
                "Тялото трябва да съдържа ключ 'грешки' с детайлите по полета");

        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrors = (Map<String, String>) response.getBody().get("грешки");
        assertTrue(fieldErrors.containsKey("egn"),
                "Грешките трябва да съдържат поле 'egn'");
        assertEquals("ЕГН-то трябва да съдържа точно 10 цифри", fieldErrors.get("egn"));
    }

    // IllegalArgumentException (бизнес грешка) трябва да върне 400
    @Test
    void handleIllegalArgument_returns400() {
        IllegalArgumentException ex =
                new IllegalArgumentException("Началната дата не може да бъде повече от 2 дни назад");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex);

        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().getMessage().contains("2 дни"));
    }
}
