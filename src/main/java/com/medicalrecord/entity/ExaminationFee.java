package com.medicalrecord.entity;

import com.medicalrecord.enums.Specialty;
import jakarta.persistence.*;
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

    @Column(name = "base_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFee;
}
