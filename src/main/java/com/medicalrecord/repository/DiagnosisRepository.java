package com.medicalrecord.repository;

import com.medicalrecord.entity.Diagnosis;
import com.medicalrecord.enums.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {

    boolean existsByCode(String code);

    Optional<Diagnosis> findByCode(String code);

    List<Diagnosis> findBySpecialtiesContaining(Specialty specialty);
}
