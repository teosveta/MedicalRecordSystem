package com.medicalrecord.mapper;

import com.medicalrecord.dto.doctor.DoctorResponse;
import com.medicalrecord.entity.Doctor;
import org.springframework.stereotype.Component;

@Component
public class DoctorMapper {

    public DoctorResponse toResponse(Doctor doctor) {
        DoctorResponse response = new DoctorResponse();
        response.setId(doctor.getId());
        response.setUniqueIdentificationNumber(doctor.getUniqueIdentificationNumber());
        response.setFirstName(doctor.getFirstName());
        response.setLastName(doctor.getLastName());
        response.setSpecialty(doctor.getSpecialty());
        response.setCanBeGP(doctor.isCanBeGP());
        if (doctor.getUser() != null) {
            response.setEmail(doctor.getUser().getUsername());
        }
        return response;
    }
}
