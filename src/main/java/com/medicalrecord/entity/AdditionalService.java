package com.medicalrecord.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

// Допълнителни медицински услуги извън стандартните специалности (напр. вземане на проби, ЕКГ)
@Entity
@Table(name = "additional_services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdditionalService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Името на услугата е задължително")
    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @NotNull(message = "Таксата е задължителна")
    @DecimalMin(value = "0.01", message = "Таксата трябва да бъде положително число")
    @Column(name = "fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal fee;
}
