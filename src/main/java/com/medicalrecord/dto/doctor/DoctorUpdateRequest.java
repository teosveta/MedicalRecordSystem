package com.medicalrecord.dto.doctor;

import com.medicalrecord.enums.Specialty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class DoctorUpdateRequest {

    @NotBlank(message = "Уникалният идентификационен номер е задължителен")
    @Pattern(regexp = "\\d{10}", message = "УИН трябва да съдържа точно 10 цифри")
    private String uniqueIdentificationNumber;

    @NotBlank(message = "Името е задължително")
    @Size(min = 2, max = 100, message = "Името трябва да е между 2 и 100 символа")
    private String firstName;

    @NotBlank(message = "Фамилията е задължителна")
    @Size(min = 2, max = 100, message = "Фамилията трябва да е между 2 и 100 символа")
    private String lastName;

    @NotEmpty(message = "Поне една специалност е задължителна")
    private Set<Specialty> specialties;

    private boolean canBeGP;
}
