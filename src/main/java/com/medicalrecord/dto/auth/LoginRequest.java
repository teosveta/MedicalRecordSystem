package com.medicalrecord.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Имейлът е задължителен")
    private String username;

    @NotBlank(message = "Паролата е задължителна")
    private String password;
}
