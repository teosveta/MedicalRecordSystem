package com.medicalrecord.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "examinations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Examination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Датата на прегледа е задължителна")
    @Column(name = "examination_date", nullable = false)
    private LocalDate examinationDate;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnosis_id", nullable = false)
    private Diagnosis diagnosis;

    @NotBlank(message = "Лечението е задължително")
    @Size(max = 2000, message = "Описанието на лечението не може да надвишава 2000 символа")
    @Column(name = "treatment", columnDefinition = "TEXT")
    private String treatment;

    @NotNull(message = "Цената е задължителна")
    @DecimalMin(value = "0.01", message = "Цената трябва да бъде положително число")
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // Пациентът плаща ако НЕ е здравноосигурен — изчислява се автоматично при запис
    @Column(name = "paid_by_patient", nullable = false)
    private boolean paidByPatient;

    // Бележка на пациента към прегледа — незадължително поле
    @Column(name = "patient_note", columnDefinition = "TEXT")
    private String patientNote;
}
