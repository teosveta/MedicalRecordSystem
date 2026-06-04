package com.medicalrecord.mapper;

import com.medicalrecord.dto.patient.PatientResponse;
import com.medicalrecord.entity.Patient;
import org.springframework.stereotype.Component;

@Component
public class PatientMapper {

    public PatientResponse toResponse(Patient patient) {
        PatientResponse response = new PatientResponse();
        response.setId(patient.getId());
        response.setFirstName(patient.getFirstName());
        response.setLastName(patient.getLastName());
        response.setEgn(patient.getEgn());
        response.setHealthInsured(patient.isHealthInsured());
        if (patient.getUser() != null) {
            response.setEmail(patient.getUser().getUsername());
        }
        // Попълваме информацията за личния лекар ако е наличен
        if (patient.getPersonalDoctor() != null) {
            response.setPersonalDoctorId(patient.getPersonalDoctor().getId());
            response.setPersonalDoctorName(
                    patient.getPersonalDoctor().getFirstName() + " " +
                    patient.getPersonalDoctor().getLastName());
        }
        return response;
    }
}
