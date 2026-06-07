package com.medicalrecord.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "sick_leaves")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SickLeave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Болничният лист задължително е свързан с преглед
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "examination_id", nullable = false)
    private Examination examination;

    @NotNull(message = "Началната дата е задължителна")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Min(value = 1, message = "Броят дни трябва да бъде поне 1")
    @Max(value = 30, message = "Болничният лист не може да е за повече от 30 дни")
    @Column(name = "number_of_days", nullable = false)
    private int numberOfDays;

    // Лекарят се взима автоматично от прегледа — не се попълва ръчно
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    // Пациентът се взима автоматично от прегледа — не се попълва ръчно
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
}
