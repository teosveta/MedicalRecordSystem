package com.medicalrecord.dto.doctor;

import com.medicalrecord.enums.Specialty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DoctorResponse {

    private Long id;
    private String uniqueIdentificationNumber;
    private String firstName;
    private String lastName;
    private Specialty specialty;
    private boolean canBeGP;
    private String email;
}
