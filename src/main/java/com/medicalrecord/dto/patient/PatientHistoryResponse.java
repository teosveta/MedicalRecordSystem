package com.medicalrecord.dto.patient;

import com.medicalrecord.dto.examination.ExaminationResponse;
import com.medicalrecord.dto.sickleave.SickLeaveResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PatientHistoryResponse {

    private PatientResponse patient;
    private List<ExaminationResponse> examinations;
    private List<SickLeaveResponse> sickLeaves;
}
