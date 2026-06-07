package com.medicalrecord.dto.patient;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatientRequest {

    @NotBlank(message = "Името е задължително")
    @Size(min = 2, max = 100, message = "Името трябва да е между 2 и 100 символа")
    private String firstName;

    @NotBlank(message = "Фамилията е задължителна")
    @Size(min = 2, max = 100, message = "Фамилията трябва да е между 2 и 100 символа")
    private String lastName;

    @NotBlank(message = "ЕГН-то е задължително")
    @Pattern(regexp = "^[0-9]{10}$", message = "ЕГН-то трябва да съдържа точно 10 цифри")
    private String egn;

    // Може да е null ако пациентът няма личен лекар
    private Long personalDoctorId;

    private boolean healthInsured;

    // Задължителен само при създаване — при редактиране се игнорира
    @Email(message = "Невалиден имейл адрес")
    private String email;

    // При редактиране се игнорира; при създаване е задължителен
    @Size(min = 7, message = "Паролата трябва да е поне 7 символа")
    private String password;
}
