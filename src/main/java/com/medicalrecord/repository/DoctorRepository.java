package com.medicalrecord.repository;

import com.medicalrecord.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByUser_Username(String username);

    boolean existsByUniqueIdentificationNumber(String uniqueIdentificationNumber);

    List<Doctor> findByCanBeGPTrue();
}
