package com.medicalrecord.dto.sickleave;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SickLeaveResponse {

    private Long id;
    private Long examinationId;
    private LocalDate startDate;
    private int numberOfDays;
    private Long doctorId;
    private String doctorName;
    private Long patientId;
    private String patientName;
}
