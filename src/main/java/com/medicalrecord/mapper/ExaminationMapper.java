package com.medicalrecord.mapper;

import com.medicalrecord.dto.examination.ExaminationResponse;
import com.medicalrecord.entity.Examination;
import org.springframework.stereotype.Component;

@Component
public class ExaminationMapper {

    public ExaminationResponse toResponse(Examination examination) {
        ExaminationResponse response = new ExaminationResponse();
        response.setId(examination.getId());
        response.setExaminationDate(examination.getExaminationDate());
        response.setTreatment(examination.getTreatment());
        response.setPrice(examination.getPrice());
        response.setPaidByPatient(examination.isPaidByPatient());
        response.setPatientNote(examination.getPatientNote());

        // Данни за лекаря
        if (examination.getDoctor() != null) {
            response.setDoctorId(examination.getDoctor().getId());
            response.setDoctorName(examination.getDoctor().getFirstName() + " " +
                                   examination.getDoctor().getLastName());
        }

        // Данни за пациента
        if (examination.getPatient() != null) {
            response.setPatientId(examination.getPatient().getId());
            response.setPatientName(examination.getPatient().getFirstName() + " " +
                                    examination.getPatient().getLastName());
        }

        // Данни за диагнозата
        if (examination.getDiagnosis() != null) {
            response.setDiagnosisId(examination.getDiagnosis().getId());
            response.setDiagnosisCode(examination.getDiagnosis().getCode());
            response.setDiagnosisName(examination.getDiagnosis().getName());
        }

        return response;
    }
}
