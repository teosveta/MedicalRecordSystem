package com.medicalrecord.service;

import com.medicalrecord.dto.patient.PatientHistoryResponse;
import com.medicalrecord.dto.patient.PatientRequest;
import com.medicalrecord.dto.patient.PatientResponse;

import java.util.List;

public interface PatientService {

    List<PatientResponse> getAllPatients();

    PatientResponse createPatient(PatientRequest request);

    PatientResponse updatePatient(Long id, PatientRequest request);

    void deletePatient(Long id);

    PatientResponse assignPersonalDoctor(Long patientId, Long doctorId);

    PatientHistoryResponse getPatientHistory(String username);
}
