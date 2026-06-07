package com.medicalrecord.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Името е задължително")
    @Size(min = 2, max = 100, message = "Името трябва да е между 2 и 100 символа")
    private String firstName;

    @NotBlank(message = "Фамилията е задължителна")
    @Size(min = 2, max = 100, message = "Фамилията трябва да е между 2 и 100 символа")
    private String lastName;

    @NotBlank(message = "ЕГН-то е задължително")
    @Pattern(regexp = "^[0-9]{10}$", message = "ЕГН-то трябва да съдържа точно 10 цифри")
    private String egn;

    @NotBlank(message = "Имейлът е задължителен")
    @Email(message = "Невалиден имейл адрес")
    private String username;

    @NotBlank(message = "Паролата е задължителна")
    @Size(min = 7, message = "Паролата трябва да е поне 7 символа")
    private String password;

    // Незадължителен — пациентът може да избере личен лекар при регистрация
    private Long personalDoctorId;
}
