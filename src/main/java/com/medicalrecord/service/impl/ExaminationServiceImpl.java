package com.medicalrecord.service.impl;

import com.medicalrecord.dto.examination.ExaminationRequest;
import com.medicalrecord.dto.examination.ExaminationResponse;
import com.medicalrecord.entity.Patient;
import com.medicalrecord.entity.*;
import com.medicalrecord.enums.Role;
import com.medicalrecord.exception.ResourceNotFoundException;
import com.medicalrecord.mapper.ExaminationMapper;
import com.medicalrecord.repository.*;
import com.medicalrecord.service.ExaminationService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExaminationServiceImpl implements ExaminationService {

    private final ExaminationRepository examinationRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final UserRepository userRepository;
    private final SickLeaveRepository sickLeaveRepository;
    private final ExaminationFeeRepository examinationFeeRepository;
    private final ExaminationMapper examinationMapper;

    public ExaminationServiceImpl(ExaminationRepository examinationRepository,
                                  DoctorRepository doctorRepository,
                                  PatientRepository patientRepository,
                                  DiagnosisRepository diagnosisRepository,
                                  UserRepository userRepository,
                                  SickLeaveRepository sickLeaveRepository,
                                  ExaminationFeeRepository examinationFeeRepository,
                                  ExaminationMapper examinationMapper) {
        this.examinationRepository = examinationRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.diagnosisRepository = diagnosisRepository;
        this.userRepository = userRepository;
        this.sickLeaveRepository = sickLeaveRepository;
        this.examinationFeeRepository = examinationFeeRepository;
        this.examinationMapper = examinationMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExaminationResponse> getExaminations(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Потребител", "username", username));

        // Пациентът вижда само своите прегледи
        if (user.getRole() == Role.PATIENT) {
            Patient patient = patientRepository.findByUser_Username(username)
                    .orElseThrow(() -> new ResourceNotFoundException("Пациент", "username", username));
            return examinationRepository.findByPatient(patient).stream()
                    .map(examinationMapper::toResponse)
                    .collect(Collectors.toList());
        }

        // Администраторът и лекарят виждат всички прегледи
        return examinationRepository.findAll().stream()
                .map(examinationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ExaminationResponse createExamination(ExaminationRequest request, String doctorUsername) {
        // Лекарят се взима от JWT токена, не от заявката
        Doctor doctor = doctorRepository.findByUser_Username(doctorUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Лекар", "username", doctorUsername));

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Пациент", "id", request.getPatientId()));

        Diagnosis diagnosis = diagnosisRepository.findById(request.getDiagnosisId())
                .orElseThrow(() -> new ResourceNotFoundException("Диагноза", "id", request.getDiagnosisId()));

        // Проверяваме дали пациентът е здравноосигурен, за да определим кой плаща
        boolean paidByPatient = !patient.isHealthInsured();

        // Цената се взима автоматично от таксата за специалността на лекаря
        java.math.BigDecimal price = examinationFeeRepository
                .findBySpecialty(doctor.getSpecialty())
                .map(com.medicalrecord.entity.ExaminationFee::getBaseFee)
                .orElse(request.getPrice());

        Examination examination = Examination.builder()
                .examinationDate(request.getExaminationDate())
                .doctor(doctor)
                .patient(patient)
                .diagnosis(diagnosis)
                .treatment(request.getTreatment())
                .price(price)
                .paidByPatient(paidByPatient)
                .build();

        return examinationMapper.toResponse(examinationRepository.save(examination));
    }

    @Override
    public ExaminationResponse updateExamination(Long id, ExaminationRequest request, String username) {
        Examination examination = examinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Преглед", "id", id));

        // Лекарят може да редактира само свои прегледи
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Потребител", "username", username));

        if (user.getRole() == Role.DOCTOR) {
            Doctor doctor = doctorRepository.findByUser_Username(username)
                    .orElseThrow(() -> new ResourceNotFoundException("Лекар", "username", username));
            if (!examination.getDoctor().getId().equals(doctor.getId())) {
                throw new AccessDeniedException("Можете да редактирате само свои прегледи");
            }
        }

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Пациент", "id", request.getPatientId()));

        Diagnosis diagnosis = diagnosisRepository.findById(request.getDiagnosisId())
                .orElseThrow(() -> new ResourceNotFoundException("Диагноза", "id", request.getDiagnosisId()));

        // Цената се взима автоматично от таксата за специалността на лекаря
        java.math.BigDecimal price = examinationFeeRepository
                .findBySpecialty(examination.getDoctor().getSpecialty())
                .map(com.medicalrecord.entity.ExaminationFee::getBaseFee)
                .orElse(request.getPrice());

        examination.setExaminationDate(request.getExaminationDate());
        examination.setPatient(patient);
        examination.setDiagnosis(diagnosis);
        examination.setTreatment(request.getTreatment());
        examination.setPrice(price);
        // Преизчисляваме плащането при промяна на пациента
        examination.setPaidByPatient(!patient.isHealthInsured());

        return examinationMapper.toResponse(examinationRepository.save(examination));
    }

    @Override
    public void deleteExamination(Long id, String username) {
        Examination examination = examinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Преглед", "id", id));

        // Лекарят може да изтрива само свои прегледи
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Потребител", "username", username));

        if (user.getRole() == Role.DOCTOR) {
            Doctor doctor = doctorRepository.findByUser_Username(username)
                    .orElseThrow(() -> new ResourceNotFoundException("Лекар", "username", username));
            if (!examination.getDoctor().getId().equals(doctor.getId())) {
                throw new AccessDeniedException("Можете да изтривате само свои прегледи");
            }
        }

        // Изтриваме свързаните болнични листове преди прегледа
        sickLeaveRepository.deleteAll(sickLeaveRepository.findAllByExamination(examination));
        examinationRepository.delete(examination);
    }

    @Override
    public ExaminationResponse updatePatientNote(Long id, String note, String patientUsername) {
        Examination examination = examinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Преглед", "id", id));

        // Пациентът може да добавя бележки само към своите прегледи
        Patient patient = patientRepository.findByUser_Username(patientUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Пациент", "username", patientUsername));

        if (!examination.getPatient().getId().equals(patient.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Можете да добавяте бележки само към свои прегледи");
        }

        examination.setPatientNote(note);
        return examinationMapper.toResponse(examinationRepository.save(examination));
    }
}
