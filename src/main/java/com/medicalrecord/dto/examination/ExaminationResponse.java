package com.medicalrecord.dto.examination;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ExaminationResponse {

    private Long id;
    private LocalDate examinationDate;
    private Long doctorId;
    private String doctorName;
    private Long patientId;
    private String patientName;
    private Long diagnosisId;
    private String diagnosisCode;
    private String diagnosisName;
    private String treatment;
    private BigDecimal price;
    private boolean paidByPatient;
    private String patientNote;
}
