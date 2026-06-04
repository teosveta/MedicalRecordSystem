package com.medicalrecord.service.impl;

import com.medicalrecord.dto.examination.ExaminationFeeResponse;
import com.medicalrecord.entity.ExaminationFee;
import com.medicalrecord.enums.Specialty;
import com.medicalrecord.exception.ResourceNotFoundException;
import com.medicalrecord.repository.ExaminationFeeRepository;
import com.medicalrecord.service.ExaminationFeeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExaminationFeeServiceImpl implements ExaminationFeeService {

    private final ExaminationFeeRepository examinationFeeRepository;

    public ExaminationFeeServiceImpl(ExaminationFeeRepository examinationFeeRepository) {
        this.examinationFeeRepository = examinationFeeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExaminationFeeResponse> getAllFees() {
        return examinationFeeRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ExaminationFeeResponse updateFee(Specialty specialty, BigDecimal baseFee) {
        ExaminationFee fee = examinationFeeRepository.findBySpecialty(specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Такса", "специалност", specialty.name()));

        fee.setBaseFee(baseFee);
        return toResponse(examinationFeeRepository.save(fee));
    }

    private ExaminationFeeResponse toResponse(ExaminationFee fee) {
        ExaminationFeeResponse response = new ExaminationFeeResponse();
        response.setSpecialty(fee.getSpecialty().name());
        response.setSpecialtyLabel(fee.getSpecialty().getLabel());
        response.setBaseFee(fee.getBaseFee());
        return response;
    }
}
