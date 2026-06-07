package com.medicalrecord.service.impl;

import com.medicalrecord.dto.examination.ExaminationFeeResponse;
import com.medicalrecord.dto.examination.FeeOptionResponse;
import com.medicalrecord.entity.Doctor;
import com.medicalrecord.entity.ExaminationFee;
import com.medicalrecord.enums.Specialty;
import com.medicalrecord.exception.ResourceNotFoundException;
import com.medicalrecord.repository.AdditionalServiceRepository;
import com.medicalrecord.repository.DoctorRepository;
import com.medicalrecord.repository.ExaminationFeeRepository;
import com.medicalrecord.service.ExaminationFeeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExaminationFeeServiceImpl implements ExaminationFeeService {

    private final ExaminationFeeRepository examinationFeeRepository;
    private final DoctorRepository doctorRepository;
    private final AdditionalServiceRepository additionalServiceRepository;

    public ExaminationFeeServiceImpl(ExaminationFeeRepository examinationFeeRepository,
                                     DoctorRepository doctorRepository,
                                     AdditionalServiceRepository additionalServiceRepository) {
        this.examinationFeeRepository = examinationFeeRepository;
        this.doctorRepository = doctorRepository;
        this.additionalServiceRepository = additionalServiceRepository;
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

    @Override
    @Transactional(readOnly = true)
    public List<FeeOptionResponse> getAvailableFees(String doctorUsername) {
        Doctor doctor = doctorRepository.findByUser_Username(doctorUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Лекар", "username", doctorUsername));

        List<FeeOptionResponse> options = new ArrayList<>();

        // Таксата за всяка специалност на лекаря
        doctor.getSpecialties().forEach(specialty ->
            examinationFeeRepository.findBySpecialty(specialty).ifPresent(f -> {
                FeeOptionResponse opt = new FeeOptionResponse();
                opt.setLabel(f.getSpecialty().getLabel() + " (консултация)");
                opt.setFee(f.getBaseFee());
                opt.setGroup("SPECIALTY");
                options.add(opt);
            })
        );

        // Таксата за ОПЛ — само ако лекарят може да бъде личен лекар и не е вече ОПЛ
        if (doctor.isCanBeGP() && !doctor.getSpecialties().contains(Specialty.GP)) {
            examinationFeeRepository.findBySpecialty(Specialty.GP).ifPresent(f -> {
                FeeOptionResponse opt = new FeeOptionResponse();
                opt.setLabel("ОПЛ (консултация)");
                opt.setFee(f.getBaseFee());
                opt.setGroup("SPECIALTY");
                options.add(opt);
            });
        }

        // Всички допълнителни услуги (вземане на проби, ЕКГ и др.)
        additionalServiceRepository.findAll().forEach(s -> {
            FeeOptionResponse opt = new FeeOptionResponse();
            opt.setLabel(s.getName());
            opt.setFee(s.getFee());
            opt.setGroup("ADDITIONAL");
            options.add(opt);
        });

        return options;
    }

    private ExaminationFeeResponse toResponse(ExaminationFee fee) {
        ExaminationFeeResponse response = new ExaminationFeeResponse();
        response.setSpecialty(fee.getSpecialty().name());
        response.setSpecialtyLabel(fee.getSpecialty().getLabel());
        response.setBaseFee(fee.getBaseFee());
        return response;
    }
}
