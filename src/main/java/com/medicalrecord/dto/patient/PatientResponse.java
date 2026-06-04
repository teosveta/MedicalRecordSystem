package com.medicalrecord.dto.patient;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatientResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String egn;
    private Long personalDoctorId;
    private String personalDoctorName;
    private boolean healthInsured;
    private String email;
}
