package com.medicalrecord.service;

import com.medicalrecord.dto.patient.PatientRequest;
import com.medicalrecord.dto.patient.PatientResponse;
import com.medicalrecord.entity.Doctor;
import com.medicalrecord.entity.Patient;
import com.medicalrecord.entity.User;
import com.medicalrecord.enums.Role;
import com.medicalrecord.exception.ResourceNotFoundException;
import com.medicalrecord.mapper.ExaminationMapper;
import com.medicalrecord.mapper.PatientMapper;
import com.medicalrecord.mapper.SickLeaveMapper;
import com.medicalrecord.repository.*;
import com.medicalrecord.service.impl.PatientServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private UserRepository userRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private ExaminationRepository examinationRepository;
    @Mock private SickLeaveRepository sickLeaveRepository;
    @Mock private PatientMapper patientMapper;
    @Mock private ExaminationMapper examinationMapper;
    @Mock private SickLeaveMapper sickLeaveMapper;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PatientServiceImpl patientService;

    // Проверяваме дали личният лекар се задава правилно при създаване на пациент
    @Test
    void createPatient_setsPersonalDoctorCorrectly() {
        Doctor doctor = Doctor.builder()
                .id(1L).firstName("Димитър").lastName("Петров").canBeGP(true).build();

        PatientRequest request = new PatientRequest();
        request.setFirstName("Иван");
        request.setLastName("Колев");
        request.setEgn("8501011234");
        request.setHealthInsured(true);
        request.setEmail("ivan@test.com");
        request.setPassword("Password123!");
        request.setPersonalDoctorId(1L);

        User savedUser = User.builder()
                .id(1L).username("ivan@test.com").role(Role.PATIENT).enabled(true).build();
        Patient savedPatient = Patient.builder()
                .id(1L).firstName("Иван").lastName("Колев")
                .egn("8501011234").personalDoctor(doctor).healthInsured(true)
                .user(savedUser).build();

        when(userRepository.existsByUsername("ivan@test.com")).thenReturn(false);
        when(patientRepository.existsByEgn("8501011234")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);
        when(patientMapper.toResponse(savedPatient)).thenReturn(new PatientResponse());

        patientService.createPatient(request);

        // Хващаме обекта изпратен за запис и проверяваме личния лекар
        ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(captor.capture());

        assertNotNull(captor.getValue().getPersonalDoctor(),
                "Личният лекар трябва да е зададен");
        assertEquals(1L, captor.getValue().getPersonalDoctor().getId(),
                "Личният лекар трябва да е Д-р Петров (id=1)");
    }

    // Проверяваме дали назначаването на нов личен лекар обновява пациента
    @Test
    void assignPersonalDoctor_updatesPatientGP() {
        Doctor newDoctor = Doctor.builder()
                .id(2L).firstName("Мария").lastName("Иванова").canBeGP(true).build();
        Patient patient = Patient.builder()
                .id(1L).firstName("Иван").lastName("Колев").egn("8501011234").build();

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(newDoctor));
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);
        when(patientMapper.toResponse(any(Patient.class))).thenReturn(new PatientResponse());

        patientService.assignPersonalDoctor(1L, 2L);

        // Проверяваме дали личният лекар е актуализиран в обекта
        assertEquals(newDoctor, patient.getPersonalDoctor(),
                "Личният лекар трябва да е актуализиран след assignPersonalDoctor");
    }

    // Проверяваме дали хвърля изключение при несъществуващ пациент
    @Test
    void assignPersonalDoctor_throwsException_whenPatientNotFound() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> patientService.assignPersonalDoctor(99L, 1L),
                "Трябва да хвърли ResourceNotFoundException при несъществуващ пациент");
    }

    // Лекар с canBeGP=false не може да бъде назначен за личен лекар
    @Test
    void assignPersonalDoctor_throwsException_whenDoctorCannotBeGP() {
        Doctor specialist = Doctor.builder()
                .id(3L).firstName("Мария").lastName("Иванова").canBeGP(false).build();
        Patient patient = Patient.builder()
                .id(1L).firstName("Иван").lastName("Колев").egn("8501011234").build();

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(3L)).thenReturn(Optional.of(specialist));

        assertThrows(IllegalArgumentException.class,
                () -> patientService.assignPersonalDoctor(1L, 3L),
                "Трябва да хвърли IllegalArgumentException когато лекарят не може да е ОПЛ");
    }
}
