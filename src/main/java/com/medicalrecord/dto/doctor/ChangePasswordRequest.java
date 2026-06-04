package com.medicalrecord.dto.doctor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

    @NotBlank(message = "Текущата парола е задължителна")
    private String currentPassword;

    @NotBlank(message = "Новата парола е задължителна")
    @Size(min = 7, message = "Паролата трябва да е поне 7 символа")
    private String newPassword;
}
