package com.medicalrecord.mapper;

import com.medicalrecord.dto.sickleave.SickLeaveResponse;
import com.medicalrecord.entity.SickLeave;
import org.springframework.stereotype.Component;

@Component
public class SickLeaveMapper {

    public SickLeaveResponse toResponse(SickLeave sickLeave) {
        SickLeaveResponse response = new SickLeaveResponse();
        response.setId(sickLeave.getId());
        response.setStartDate(sickLeave.getStartDate());
        response.setNumberOfDays(sickLeave.getNumberOfDays());

        if (sickLeave.getExamination() != null) {
            response.setExaminationId(sickLeave.getExamination().getId());
        }

        // Лекарят и пациентът са взети от прегледа при създаване
        if (sickLeave.getDoctor() != null) {
            response.setDoctorId(sickLeave.getDoctor().getId());
            response.setDoctorName(sickLeave.getDoctor().getFirstName() + " " +
                                   sickLeave.getDoctor().getLastName());
        }

        if (sickLeave.getPatient() != null) {
            response.setPatientId(sickLeave.getPatient().getId());
            response.setPatientName(sickLeave.getPatient().getFirstName() + " " +
                                    sickLeave.getPatient().getLastName());
        }

        return response;
    }
}
