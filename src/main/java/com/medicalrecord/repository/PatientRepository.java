package com.medicalrecord.repository;

import com.medicalrecord.entity.Doctor;
import com.medicalrecord.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByUser_Username(String username);

    boolean existsByEgn(String egn);

    List<Patient> findByPersonalDoctor(Doctor doctor);

    // Брой пациенти по личен лекар (ОПЛ) — за статистика
    @Query("SELECT p.personalDoctor.id, COUNT(p) FROM Patient p WHERE p.personalDoctor IS NOT NULL GROUP BY p.personalDoctor.id ORDER BY COUNT(p) DESC")
    List<Object[]> countPatientsByPersonalDoctor();
}
