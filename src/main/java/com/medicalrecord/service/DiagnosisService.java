package com.medicalrecord.service;

import com.medicalrecord.dto.diagnosis.DiagnosisRequest;
import com.medicalrecord.dto.diagnosis.DiagnosisResponse;
import com.medicalrecord.enums.Specialty;

import java.util.List;
import java.util.Set;

public interface DiagnosisService {

    List<DiagnosisResponse> getAllDiagnoses();

    List<DiagnosisResponse> getDiagnosesBySpecialties(Set<Specialty> specialties);

    DiagnosisResponse createDiagnosis(DiagnosisRequest request);

    DiagnosisResponse updateDiagnosis(Long id, DiagnosisRequest request);

    void deleteDiagnosis(Long id);
}
