package com.medicalrecord.repository;

import com.medicalrecord.entity.ExaminationFee;
import com.medicalrecord.enums.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExaminationFeeRepository extends JpaRepository<ExaminationFee, Long> {

    Optional<ExaminationFee> findBySpecialty(Specialty specialty);

    boolean existsBySpecialty(Specialty specialty);
}
