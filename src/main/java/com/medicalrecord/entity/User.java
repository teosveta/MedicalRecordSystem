package com.medicalrecord.entity;

import com.medicalrecord.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Имейлът се използва като потребителско име
    @NotBlank(message = "Имейлът е задължителен")
    @Email(message = "Невалиден имейл адрес")
    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @NotBlank(message = "Паролата е задължителна")
    @Column(name = "password", nullable = false)
    private String password;

    @NotNull(message = "Ролята е задължителна")
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    // Обратна връзка към лекар (не е задължителна — администраторът няма лекарски профил)
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Doctor doctor;

    // Обратна връзка към пациент (не е задължителна)
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Patient patient;
}
