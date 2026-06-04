package com.medicalrecord.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    // ЕГН — уникален идентификатор, точно 10 цифри
    @Column(name = "egn", unique = true, nullable = false, length = 10)
    private String egn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_doctor_id")
    private Doctor personalDoctor;

    // Здравна осигуровка за последните 6 месеца
    @Column(name = "health_insured", nullable = false)
    private boolean healthInsured;

    // Свързан потребителски акаунт за автентикация
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY)
    private List<Examination> examinations;
}
