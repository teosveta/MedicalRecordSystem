package com.medicalrecord.entity;

import com.medicalrecord.enums.Specialty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "УИН е задължителен")
    @Pattern(regexp = "\\d{10}", message = "УИН трябва да съдържа точно 10 цифри")
    @Column(name = "unique_identification_number", unique = true, nullable = false)
    private String uniqueIdentificationNumber;

    @NotBlank(message = "Името е задължително")
    @Size(min = 2, max = 100, message = "Името трябва да е между 2 и 100 символа")
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank(message = "Фамилията е задължителна")
    @Size(min = 2, max = 100, message = "Фамилията трябва да е между 2 и 100 символа")
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @ElementCollection(targetClass = Specialty.class, fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "doctor_specialties", joinColumns = @JoinColumn(name = "doctor_id"))
    @Column(name = "specialty", nullable = false)
    @Builder.Default
    private Set<Specialty> specialties = new HashSet<>();

    // Показва дали лекарят може да бъде личен лекар (ОПЛ)
    @Column(name = "can_be_gp", nullable = false)
    private boolean canBeGP;

    // Свързан потребителски акаунт за автентикация
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @OneToMany(mappedBy = "personalDoctor", fetch = FetchType.LAZY)
    private Set<Patient> patients;

    @OneToMany(mappedBy = "doctor", fetch = FetchType.LAZY)
    private Set<Examination> examinations;
}
