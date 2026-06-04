package com.medicalrecord.mapper;

import com.medicalrecord.dto.diagnosis.DiagnosisResponse;
import com.medicalrecord.entity.Diagnosis;
import org.springframework.stereotype.Component;

@Component
public class DiagnosisMapper {

    public DiagnosisResponse toResponse(Diagnosis diagnosis) {
        DiagnosisResponse response = new DiagnosisResponse();
        response.setId(diagnosis.getId());
        response.setCode(diagnosis.getCode());
        response.setName(diagnosis.getName());
        response.setDescription(diagnosis.getDescription());
        return response;
    }
}
