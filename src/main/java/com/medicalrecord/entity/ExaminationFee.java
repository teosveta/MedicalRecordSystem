package com.medicalrecord.entity;

import com.medicalrecord.enums.Specialty;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "examination_fees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExaminationFee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Всяка специалност има точно една базова такса
    @Enumerated(EnumType.STRING)
    @Column(name = "specialty", unique = true, nullable = false)
    private Specialty specialty;

    @NotNull(message = "Базовата такса е задължителна")
    @DecimalMin(value = "0.01", message = "Таксата трябва да бъде положително число")
    @Column(name = "base_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFee;
}
