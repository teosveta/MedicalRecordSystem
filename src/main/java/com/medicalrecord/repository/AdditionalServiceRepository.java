package com.medicalrecord.repository;

import com.medicalrecord.entity.AdditionalService;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdditionalServiceRepository extends JpaRepository<AdditionalService, Long> {

    boolean existsByName(String name);
}
