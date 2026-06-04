package com.medicalrecord.service;

import com.medicalrecord.dto.examination.ExaminationRequest;
import com.medicalrecord.dto.examination.ExaminationResponse;

import java.util.List;

public interface ExaminationService {

    List<ExaminationResponse> getExaminations(String username);

    ExaminationResponse createExamination(ExaminationRequest request, String doctorUsername);

    ExaminationResponse updateExamination(Long id, ExaminationRequest request, String username);

    void deleteExamination(Long id, String username);

    ExaminationResponse updatePatientNote(Long id, String note, String patientUsername);
}
