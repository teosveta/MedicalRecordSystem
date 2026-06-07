package com.medicalrecord.dto.diagnosis;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiagnosisRequest {

    @NotBlank(message = "Кодът на диагнозата е задължителен")
    @Size(max = 10, message = "МКБ-10 кодът не може да надвишава 10 символа")
    private String code;

    @NotBlank(message = "Името на диагнозата е задължително")
    @Size(max = 255, message = "Името на диагнозата не може да надвишава 255 символа")
    private String name;

    private String description;
}
