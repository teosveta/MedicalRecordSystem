package com.medicalrecord.dto.examination;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ExaminationRequest {

    @NotNull(message = "Датата на прегледа е задължителна")
    @PastOrPresent(message = "Датата на прегледа не може да бъде в бъдещето")
    private LocalDate examinationDate;

    // Лекарят НЕ е в заявката — взима се автоматично от JWT токена
    @NotNull(message = "Пациентът е задължителен")
    private Long patientId;

    @NotNull(message = "Диагнозата е задължителна")
    private Long diagnosisId;

    @NotBlank(message = "Лечението е задължително")
    @Size(max = 2000, message = "Описанието на лечението не може да надвишава 2000 символа")
    private String treatment;

    @NotNull(message = "Цената е задължителна")
    @DecimalMin(value = "0.01", message = "Цената трябва да бъде положително число")
    private BigDecimal price;
}
