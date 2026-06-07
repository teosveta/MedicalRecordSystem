package com.medicalrecord.service.impl;

import com.medicalrecord.dto.sickleave.SickLeaveRequest;
import com.medicalrecord.dto.sickleave.SickLeaveResponse;
import com.medicalrecord.dto.sickleave.UpdateSickLeaveRequest;
import com.medicalrecord.entity.*;
import com.medicalrecord.enums.Role;
import com.medicalrecord.exception.ResourceNotFoundException;
import com.medicalrecord.mapper.SickLeaveMapper;
import com.medicalrecord.repository.*;
import com.medicalrecord.service.SickLeaveService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SickLeaveServiceImpl implements SickLeaveService {

    private final SickLeaveRepository sickLeaveRepository;
    private final ExaminationRepository examinationRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final SickLeaveMapper sickLeaveMapper;

    public SickLeaveServiceImpl(SickLeaveRepository sickLeaveRepository,
                                ExaminationRepository examinationRepository,
                                DoctorRepository doctorRepository,
                                PatientRepository patientRepository,
                                UserRepository userRepository,
                                SickLeaveMapper sickLeaveMapper) {
        this.sickLeaveRepository = sickLeaveRepository;
        this.examinationRepository = examinationRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.sickLeaveMapper = sickLeaveMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SickLeaveResponse> getSickLeaves(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Потребител", "username", username));

        // Пациентът вижда само своите болнични листове
        if (user.getRole() == Role.PATIENT) {
            Patient patient = patientRepository.findByUser_Username(username)
                    .orElseThrow(() -> new ResourceNotFoundException("Пациент", "username", username));
            return sickLeaveRepository.findByPatient(patient).stream()
                    .map(sickLeaveMapper::toResponse)
                    .collect(Collectors.toList());
        }

        // Лекарят вижда само своите болнични листове
        if (user.getRole() == Role.DOCTOR) {
            Doctor doctor = doctorRepository.findByUser_Username(username)
                    .orElseThrow(() -> new ResourceNotFoundException("Лекар", "username", username));
            return sickLeaveRepository.findByDoctor(doctor).stream()
                    .map(sickLeaveMapper::toResponse)
                    .collect(Collectors.toList());
        }

        // Администраторът вижда всички болнични листове
        return sickLeaveRepository.findAll().stream()
                .map(sickLeaveMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SickLeaveResponse createSickLeave(SickLeaveRequest request, String doctorUsername) {
        Examination examination = examinationRepository.findById(request.getExaminationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Преглед", "id", request.getExaminationId()));

        // Лекарят може да издава болничен лист само за собствен преглед
        Doctor currentDoctor = doctorRepository.findByUser_Username(doctorUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Лекар", "username", doctorUsername));
        if (!examination.getDoctor().getId().equals(currentDoctor.getId())) {
            throw new AccessDeniedException("Можете да издавате болнични листове само за свои прегледи");
        }

        // Не може да се издаде втори болничен лист за същия преглед
        if (sickLeaveRepository.existsByExaminationId(examination.getId())) {
            throw new IllegalArgumentException(
                    "За този преглед вече е издаден болничен лист");
        }

        // Прегледът не може да е по-стар от 7 дни
        if (examination.getExaminationDate().isBefore(LocalDate.now().minusDays(7))) {
            throw new IllegalArgumentException(
                    "Болничен лист може да се издаде само за преглед от последните 7 дни");
        }

        // Допълнителна проверка в сервиза — началната дата не може да е повече от 2 дни назад
        if (request.getStartDate().isBefore(LocalDate.now().minusDays(2))) {
            throw new IllegalArgumentException(
                    "Началната дата не може да бъде повече от 2 дни назад");
        }

        // Болничният лист не може да е за повече от 30 дни
        if (request.getNumberOfDays() > 30) {
            throw new IllegalArgumentException(
                    "Болничният лист не може да е за повече от 30 дни");
        }

        // Лекарят и пациентът се взимат автоматично от прегледа
        SickLeave sickLeave = SickLeave.builder()
                .examination(examination)
                .startDate(request.getStartDate())
                .numberOfDays(request.getNumberOfDays())
                .doctor(examination.getDoctor())
                .patient(examination.getPatient())
                .build();

        return sickLeaveMapper.toResponse(sickLeaveRepository.save(sickLeave));
    }

    @Override
    public SickLeaveResponse updateSickLeave(Long id, UpdateSickLeaveRequest request, String username) {
        SickLeave sickLeave = sickLeaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Болничен лист", "id", id));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Потребител", "username", username));

        if (user.getRole() == Role.DOCTOR) {
            Doctor doctor = doctorRepository.findByUser_Username(username)
                    .orElseThrow(() -> new ResourceNotFoundException("Лекар", "username", username));
            if (!sickLeave.getDoctor().getId().equals(doctor.getId())) {
                throw new AccessDeniedException("Можете да редактирате само свои болнични листове");
            }
        }

        sickLeave.setStartDate(request.getStartDate());
        sickLeave.setNumberOfDays(request.getNumberOfDays());

        return sickLeaveMapper.toResponse(sickLeaveRepository.save(sickLeave));
    }

    @Override
    public void deleteSickLeave(Long id, String username) {
        SickLeave sickLeave = sickLeaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Болничен лист", "id", id));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Потребител", "username", username));

        // Лекарят може да изтрива само свои болнични листове издадени днес
        if (user.getRole() == Role.DOCTOR) {
            Doctor doctor = doctorRepository.findByUser_Username(username)
                    .orElseThrow(() -> new ResourceNotFoundException("Лекар", "username", username));
            if (!sickLeave.getDoctor().getId().equals(doctor.getId())) {
                throw new AccessDeniedException("Можете да изтривате само свои болнични листове");
            }
            if (!sickLeave.getStartDate().isEqual(LocalDate.now())) {
                throw new IllegalArgumentException("Може да изтриете само болнични листове издадени днес");
            }
        }

        sickLeaveRepository.delete(sickLeave);
    }
}
