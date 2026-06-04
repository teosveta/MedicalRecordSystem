package com.medicalrecord.entity;

import com.medicalrecord.enums.Specialty;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "diagnoses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Diagnosis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // МКБ-10 код — уникален идентификатор на диагнозата
    @Column(name = "code", unique = true, nullable = false, length = 10)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "diagnosis_specialties", joinColumns = @JoinColumn(name = "diagnosis_id"))
    @Column(name = "specialty")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<Specialty> specialties = new HashSet<>();
}
