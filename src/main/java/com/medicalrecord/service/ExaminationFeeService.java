package com.medicalrecord.service;

import com.medicalrecord.dto.examination.ExaminationFeeResponse;
import com.medicalrecord.enums.Specialty;

import java.math.BigDecimal;
import java.util.List;

public interface ExaminationFeeService {

    List<ExaminationFeeResponse> getAllFees();

    ExaminationFeeResponse updateFee(Specialty specialty, BigDecimal baseFee);
}
