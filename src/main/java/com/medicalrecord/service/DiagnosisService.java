package com.medicalrecord.service;

import com.medicalrecord.dto.diagnosis.DiagnosisRequest;
import com.medicalrecord.dto.diagnosis.DiagnosisResponse;
import com.medicalrecord.enums.Specialty;

import java.util.List;

public interface DiagnosisService {

    List<DiagnosisResponse> getAllDiagnoses();

    List<DiagnosisResponse> getDiagnosesBySpecialty(Specialty specialty);

    DiagnosisResponse createDiagnosis(DiagnosisRequest request);

    DiagnosisResponse updateDiagnosis(Long id, DiagnosisRequest request);

    void deleteDiagnosis(Long id);
}
