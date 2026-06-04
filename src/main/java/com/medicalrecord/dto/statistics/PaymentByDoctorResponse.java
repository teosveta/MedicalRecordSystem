package com.medicalrecord.dto.statistics;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentByDoctorResponse {

    private Long doctorId;
    private String doctorName;
    private BigDecimal totalAmount;
}
