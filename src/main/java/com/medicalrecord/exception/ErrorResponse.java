package com.medicalrecord.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ErrorResponse {

    private int status;

    @JsonProperty("грешка")
    private String error;

    @JsonProperty("съобщение")
    private String message;

    private LocalDateTime timestamp;
}
