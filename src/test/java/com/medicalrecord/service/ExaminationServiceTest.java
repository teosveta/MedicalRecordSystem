package com.medicalrecord.service;

import com.medicalrecord.dto.examination.ExaminationRequest;
import com.medicalrecord.entity.*;
import com.medicalrecord.enums.Role;
import com.medicalrecord.exception.ResourceNotFoundException;
import com.medicalrecord.mapper.ExaminationMapper;
import com.medicalrecord.repository.*;
import com.medicalrecord.service.impl.ExaminationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExaminationServiceTest {

    @Mock private ExaminationRepository examinationRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private DiagnosisRepository diagnosisRepository;
    @Mock private UserRepository userRepository;
    @Mock private SickLeaveRepository sickLeaveRepository;
    @Mock private ExaminationFeeRepository examinationFeeRepository;
    @Mock private ExaminationMapper examinationMapper;

    @InjectMocks
    private ExaminationServiceImpl examinationService;

    // Неосигуреният пациент трябва да плаща сам (paidByPatient = true)
    @Test
    void createExamination_paidByPatient_true_whenPatientNotInsured() {
        Doctor doctor = Doctor.builder().id(1L).firstName("Димитър").lastName("Петров").build();
        Patient patient = Patient.builder().id(1L).healthInsured(false).build(); // неосигурен
        Diagnosis diagnosis = Diagnosis.builder().id(1L).build();

        User doctorUser = User.builder().username("doctor@test.com").role(Role.DOCTOR).build();

        ExaminationRequest request = new ExaminationRequest();
        request.setExaminationDate(LocalDate.now());
        request.setPatientId(1L);
        request.setDiagnosisId(1L);
        request.setPrice(new BigDecimal("30.00"));

        Examination savedExam = Examination.builder()
                .id(1L).doctor(doctor).patient(patient).diagnosis(diagnosis)
                .paidByPatient(true).price(new BigDecimal("30.00")).build();

        when(doctorRepository.findByUser_Username("doctor@test.com")).thenReturn(Optional.of(doctor));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(diagnosisRepository.findById(1L)).thenReturn(Optional.of(diagnosis));
        when(examinationFeeRepository.findBySpecialty(any())).thenReturn(Optional.empty());
        when(examinationRepository.save(any(Examination.class))).thenReturn(savedExam);
        when(examinationMapper.toResponse(any())).thenReturn(null);

        examinationService.createExamination(request, "doctor@test.com");

        // Проверяваме дали paidByPatient е true за неосигурен пациент
        ArgumentCaptor<Examination> captor = ArgumentCaptor.forClass(Examination.class);
        verify(examinationRepository).save(captor.capture());
        assertTrue(captor.getValue().isPaidByPatient(),
                "Неосигуреният пациент трябва да плаща прегледа");
    }

    // Осигуреният пациент НЕ трябва да плаща (paidByPatient = false)
    @Test
    void createExamination_paidByPatient_false_whenPatientInsured() {
        Doctor doctor = Doctor.builder().id(1L).build();
        Patient patient = Patient.builder().id(1L).healthInsured(true).build(); // осигурен
        Diagnosis diagnosis = Diagnosis.builder().id(1L).build();

        ExaminationRequest request = new ExaminationRequest();
        request.setExaminationDate(LocalDate.now());
        request.setPatientId(1L);
        request.setDiagnosisId(1L);
        request.setPrice(new BigDecimal("20.00"));

        Examination savedExam = Examination.builder()
                .id(1L).doctor(doctor).patient(patient).diagnosis(diagnosis)
                .paidByPatient(false).price(new BigDecimal("20.00")).build();

        when(doctorRepository.findByUser_Username("doctor@test.com")).thenReturn(Optional.of(doctor));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(diagnosisRepository.findById(1L)).thenReturn(Optional.of(diagnosis));
        when(examinationFeeRepository.findBySpecialty(any())).thenReturn(Optional.empty());
        when(examinationRepository.save(any(Examination.class))).thenReturn(savedExam);
        when(examinationMapper.toResponse(any())).thenReturn(null);

        examinationService.createExamination(request, "doctor@test.com");

        ArgumentCaptor<Examination> captor = ArgumentCaptor.forClass(Examination.class);
        verify(examinationRepository).save(captor.capture());
        assertFalse(captor.getValue().isPaidByPatient(),
                "Осигуреният пациент не трябва да плаща прегледа");
    }

    // Лекарят не може да редактира чужд преглед — очакваме AccessDeniedException
    @Test
    void updateExamination_throwsAccessDenied_whenDoctorEditsOthersExamination() {
        Doctor ownerDoctor = Doctor.builder().id(1L).build();
        Doctor otherDoctor = Doctor.builder().id(2L).build();

        User otherDoctorUser = User.builder()
                .username("other@test.com").role(Role.DOCTOR).build();

        Examination examination = Examination.builder()
                .id(1L).doctor(ownerDoctor).build(); // принадлежи на doctor1

        ExaminationRequest request = new ExaminationRequest();
        request.setExaminationDate(LocalDate.now());
        request.setPatientId(1L);
        request.setDiagnosisId(1L);
        request.setPrice(BigDecimal.TEN);

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(examination));
        when(userRepository.findByUsername("other@test.com")).thenReturn(Optional.of(otherDoctorUser));
        when(doctorRepository.findByUser_Username("other@test.com")).thenReturn(Optional.of(otherDoctor));

        // Д-р other се опитва да редактира преглед на Д-р owner — трябва да хвърли грешка
        assertThrows(AccessDeniedException.class,
                () -> examinationService.updateExamination(1L, request, "other@test.com"),
                "Лекарят не трябва да може да редактира чужди прегледи");
    }

    // Несъществуващ преглед при редактиране → ResourceNotFoundException
    @Test
    void updateExamination_throwsException_whenExaminationNotFound() {
        when(examinationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> examinationService.updateExamination(99L, new ExaminationRequest(), "doctor@test.com"));
    }
}
