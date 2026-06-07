package com.medicalrecord.dto.statistics;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class PaymentByDoctorResponse {

    private Long doctorId;
    private String doctorName;
    private BigDecimal totalAmount;
    private BigDecimal insuranceAmount;
    private BigDecimal patientAmount;

    // JPQL constructor expression — CASE WHEN може да върне null ако няма редове от типа
    public PaymentByDoctorResponse(Long doctorId, String doctorName,
                                   BigDecimal totalAmount,
                                   BigDecimal insuranceAmount,
                                   BigDecimal patientAmount) {
        this.doctorId       = doctorId;
        this.doctorName     = doctorName;
        this.totalAmount    = totalAmount    != null ? totalAmount    : BigDecimal.ZERO;
        this.insuranceAmount = insuranceAmount != null ? insuranceAmount : BigDecimal.ZERO;
        this.patientAmount  = patientAmount  != null ? patientAmount  : BigDecimal.ZERO;
    }
}
