package com.medicalrecord.dto.doctor;

import com.medicalrecord.enums.Specialty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DoctorRequest {

    @NotBlank(message = "Уникалният идентификационен номер е задължителен")
    private String uniqueIdentificationNumber;

    @NotBlank(message = "Името е задължително")
    @Size(max = 100, message = "Името не може да надвишава 100 символа")
    private String firstName;

    @NotBlank(message = "Фамилията е задължителна")
    @Size(max = 100, message = "Фамилията не може да надвишава 100 символа")
    private String lastName;

    @NotNull(message = "Специалността е задължителна")
    private Specialty specialty;

    private boolean canBeGP;

    @NotBlank(message = "Имейлът е задължителен")
    @Email(message = "Невалиден имейл адрес")
    private String email;

    @NotBlank(message = "Паролата е задължителна")
    @Size(min = 7, message = "Паролата трябва да е поне 7 символа")
    private String password;
}
