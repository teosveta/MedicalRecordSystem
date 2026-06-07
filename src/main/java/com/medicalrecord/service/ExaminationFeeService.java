package com.medicalrecord.service;

import com.medicalrecord.dto.examination.ExaminationFeeResponse;
import com.medicalrecord.dto.examination.FeeOptionResponse;
import com.medicalrecord.enums.Specialty;

import java.math.BigDecimal;
import java.util.List;

public interface ExaminationFeeService {

    List<ExaminationFeeResponse> getAllFees();

    ExaminationFeeResponse updateFee(Specialty specialty, BigDecimal baseFee);

    // Връща таксите достъпни за конкретния лекар — своята специалност, ОПЛ (ако canBeGP) и допълнителни услуги
    List<FeeOptionResponse> getAvailableFees(String doctorUsername);
}
