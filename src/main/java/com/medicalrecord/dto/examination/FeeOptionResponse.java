package com.medicalrecord.dto.examination;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

// Един вариант за такса, показан в dropdown-а при създаване/редактиране на преглед
@Getter
@Setter
public class FeeOptionResponse {

    // Текст за показване в dropdown-а (напр. "Кардиолог (консултация)")
    private String label;

    // Стойността на таксата
    private BigDecimal fee;

    // "SPECIALTY" — такса по специалност; "ADDITIONAL" — допълнителна услуга
    private String group;
}
