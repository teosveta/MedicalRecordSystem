package com.medicalrecord.entity;

import com.medicalrecord.enums.Specialty;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

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

    @Column(name = "unique_identification_number", unique = true, nullable = false)
    private String uniqueIdentificationNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "specialty", nullable = false)
    private Specialty specialty;

    // Показва дали лекарят може да бъде личен лекар (ОПЛ)
    @Column(name = "can_be_gp", nullable = false)
    private boolean canBeGP;

    // Свързан потребителски акаунт за автентикация
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @OneToMany(mappedBy = "personalDoctor", fetch = FetchType.LAZY)
    private List<Patient> patients;

    @OneToMany(mappedBy = "doctor", fetch = FetchType.LAZY)
    private List<Examination> examinations;
}
