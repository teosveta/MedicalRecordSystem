package com.medicalrecord.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;

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

    @NotBlank(message = "Името е задължително")
    @Size(min = 2, max = 100, message = "Името трябва да е между 2 и 100 символа")
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank(message = "Фамилията е задължителна")
    @Size(min = 2, max = 100, message = "Фамилията трябва да е между 2 и 100 символа")
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    // ЕГН — уникален идентификатор, точно 10 цифри
    @NotBlank(message = "ЕГН-то е задължително")
    @Pattern(regexp = "^[0-9]{10}$", message = "ЕГН-то трябва да съдържа точно 10 цифри")
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
    private Set<Examination> examinations;
}
