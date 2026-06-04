package com.medicalrecord.dto.diagnosis;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiagnosisResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
}
