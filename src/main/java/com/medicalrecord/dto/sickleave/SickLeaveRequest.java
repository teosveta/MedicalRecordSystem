package com.medicalrecord.dto.sickleave;

import com.medicalrecord.validation.NotTooOld;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SickLeaveRequest {

    // Лекарят и пациентът се взимат автоматично от прегледа
    @NotNull(message = "Прегледът е задължителен")
    private Long examinationId;

    @NotNull(message = "Началната дата е задължителна")
    @NotTooOld
    private LocalDate startDate;

    @NotNull(message = "Броят дни е задължителен")
    @Min(value = 1, message = "Броят дни трябва да бъде поне 1")
    @Max(value = 30, message = "Болничният лист не може да е за повече от 30 дни")
    private Integer numberOfDays;
}
