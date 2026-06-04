package com.medicalrecord.dto.examination;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ExaminationFeeResponse {

    private String specialty;
    private String specialtyLabel;
    private BigDecimal baseFee;
}
