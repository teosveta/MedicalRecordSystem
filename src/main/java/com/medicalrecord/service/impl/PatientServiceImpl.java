package com.medicalrecord.service.impl;

import com.medicalrecord.dto.examination.ExaminationResponse;
import com.medicalrecord.dto.patient.PatientHistoryResponse;
import com.medicalrecord.dto.patient.PatientRequest;
import com.medicalrecord.dto.patient.PatientResponse;
import com.medicalrecord.dto.sickleave.SickLeaveResponse;
import com.medicalrecord.entity.Doctor;
import com.medicalrecord.entity.Examination;
import com.medicalrecord.entity.Patient;
import com.medicalrecord.entity.User;
import com.medicalrecord.enums.Role;
import com.medicalrecord.exception.ResourceNotFoundException;
import com.medicalrecord.mapper.ExaminationMapper;
import com.medicalrecord.mapper.PatientMapper;
import com.medicalrecord.mapper.SickLeaveMapper;
import com.medicalrecord.repository.*;
import com.medicalrecord.service.PatientService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final ExaminationRepository examinationRepository;
    private final SickLeaveRepository sickLeaveRepository;
    private final PatientMapper patientMapper;
    private final ExaminationMapper examinationMapper;
    private final SickLeaveMapper sickLeaveMapper;
    private final PasswordEncoder passwordEncoder;

    public PatientServiceImpl(PatientRepository patientRepository,
                              UserRepository userRepository,
                              DoctorRepository doctorRepository,
                              ExaminationRepository examinationRepository,
                              SickLeaveRepository sickLeaveRepository,
                              PatientMapper patientMapper,
                              ExaminationMapper examinationMapper,
                              SickLeaveMapper sickLeaveMapper,
                              PasswordEncoder passwordEncoder) {
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.examinationRepository = examinationRepository;
        this.sickLeaveRepository = sickLeaveRepository;
        this.patientMapper = patientMapper;
        this.examinationMapper = examinationMapper;
        this.sickLeaveMapper = sickLeaveMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientResponse> getAllPatients() {
        return patientRepository.findAll().stream()
                .map(patientMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PatientResponse createPatient(PatientRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Имейлът е задължителен при създаване на пациент");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Паролата е задължителна при създаване на пациент");
        }
        if (userRepository.existsByUsername(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Потребител с имейл '" + request.getEmail() + "' вече съществува");
        }
        if (patientRepository.existsByEgn(request.getEgn())) {
            throw new IllegalArgumentException(
                    "Пациент с ЕГН '" + request.getEgn() + "' вече съществува");
        }

        User user = User.builder()
                .username(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.PATIENT)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        Doctor personalDoctor = null;
        if (request.getPersonalDoctorId() != null) {
            personalDoctor = doctorRepository.findById(request.getPersonalDoctorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Лекар", "id", request.getPersonalDoctorId()));

            // Проверяваме дали лекарят може да бъде личен лекар (ОПЛ)
            if (!personalDoctor.isCanBeGP()) {
                throw new IllegalArgumentException(
                        "Д-р " + personalDoctor.getLastName() + " не може да бъде личен лекар");
            }
        }

        Patient patient = Patient.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .egn(request.getEgn())
                .personalDoctor(personalDoctor)
                .healthInsured(request.isHealthInsured())
                .user(savedUser)
                .build();

        return patientMapper.toResponse(patientRepository.save(patient));
    }

    @Override
    public PatientResponse updatePatient(Long id, PatientRequest request) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пациент", "id", id));

        // Проверяваме за дублиране на ЕГН само ако е различно от текущото
        if (!patient.getEgn().equals(request.getEgn()) && patientRepository.existsByEgn(request.getEgn())) {
            throw new IllegalArgumentException(
                    "Пациент с ЕГН '" + request.getEgn() + "' вече съществува");
        }

        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setEgn(request.getEgn());
        patient.setHealthInsured(request.isHealthInsured());

        if (request.getPersonalDoctorId() != null) {
            Doctor personalDoctor = doctorRepository.findById(request.getPersonalDoctorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Лекар", "id", request.getPersonalDoctorId()));
            if (!personalDoctor.isCanBeGP()) {
                throw new IllegalArgumentException(
                        "Д-р " + personalDoctor.getLastName() + " не може да бъде личен лекар");
            }
            patient.setPersonalDoctor(personalDoctor);
        } else {
            patient.setPersonalDoctor(null);
        }

        return patientMapper.toResponse(patientRepository.save(patient));
    }

    @Override
    public void deletePatient(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пациент", "id", id));

        // Изтриваме болничните листове и прегледите на пациента
        List<Examination> examinations = examinationRepository.findByPatient(patient);
        sickLeaveRepository.deleteAllByExaminationIn(examinations);
        examinationRepository.deleteAll(examinations);

        User user = patient.getUser();
        patientRepository.delete(patient);
        if (user != null) {
            userRepository.delete(user);
        }
    }

    @Override
    public PatientResponse assignPersonalDoctor(Long patientId, Long doctorId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Пациент", "id", patientId));
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Лекар", "id", doctorId));

        // Проверяваме дали лекарят може да бъде личен лекар
        if (!doctor.isCanBeGP()) {
            throw new IllegalArgumentException(
                    "Д-р " + doctor.getLastName() + " не може да бъде личен лекар (ОПЛ)");
        }

        patient.setPersonalDoctor(doctor);
        return patientMapper.toResponse(patientRepository.save(patient));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExaminationResponse> getPatientExaminationsById(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Пациент", "id", patientId));
        return examinationRepository.findByPatient(patient).stream()
                .map(examinationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SickLeaveResponse> getPatientSickLeavesById(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Пациент", "id", patientId));
        return sickLeaveRepository.findByPatient(patient).stream()
                .map(sickLeaveMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PatientHistoryResponse getPatientHistory(String username) {
        Patient patient = patientRepository.findByUser_Username(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пациент", "username", username));

        List<ExaminationResponse> examinations = examinationRepository.findByPatient(patient).stream()
                .map(examinationMapper::toResponse)
                .collect(Collectors.toList());

        List<SickLeaveResponse> sickLeaves = sickLeaveRepository.findByPatient(patient).stream()
                .map(sickLeaveMapper::toResponse)
                .collect(Collectors.toList());

        return PatientHistoryResponse.builder()
                .patient(patientMapper.toResponse(patient))
                .examinations(examinations)
                .sickLeaves(sickLeaves)
                .build();
    }
}
