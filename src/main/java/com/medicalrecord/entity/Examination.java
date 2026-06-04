package com.medicalrecord.entity;

import jakarta.persistence.*;
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

    @Column(name = "treatment", columnDefinition = "TEXT")
    private String treatment;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // Пациентът плаща ако НЕ е здравноосигурен — изчислява се автоматично при запис
    @Column(name = "paid_by_patient", nullable = false)
    private boolean paidByPatient;

    // Бележка на пациента към прегледа — незадължително поле
    @Column(name = "patient_note", columnDefinition = "TEXT")
    private String patientNote;
}
