package com.medicalrecord.dto.diagnosis;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiagnosisRequest {

    @NotBlank(message = "Кодът на диагнозата е задължителен")
    private String code;

    @NotBlank(message = "Името на диагнозата е задължително")
    private String name;

    private String description;
}
