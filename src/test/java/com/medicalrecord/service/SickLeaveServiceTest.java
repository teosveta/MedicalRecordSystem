package com.medicalrecord.service;

import com.medicalrecord.dto.sickleave.SickLeaveRequest;
import com.medicalrecord.entity.Doctor;
import com.medicalrecord.entity.Examination;
import com.medicalrecord.entity.Patient;
import com.medicalrecord.entity.SickLeave;
import com.medicalrecord.exception.ResourceNotFoundException;
import com.medicalrecord.mapper.SickLeaveMapper;
import com.medicalrecord.repository.*;
import com.medicalrecord.service.impl.SickLeaveServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SickLeaveServiceTest {

    @Mock private SickLeaveRepository sickLeaveRepository;
    @Mock private ExaminationRepository examinationRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private UserRepository userRepository;
    @Mock private SickLeaveMapper sickLeaveMapper;

    @InjectMocks
    private SickLeaveServiceImpl sickLeaveService;

    // Начална дата повече от 2 дни назад трябва да хвърли IllegalArgumentException
    @Test
    void createSickLeave_throwsException_whenStartDateMoreThan2DaysAgo() {
        Doctor doctor = Doctor.builder().id(1L).build();
        Patient patient = Patient.builder().id(1L).build();
        Examination examination = Examination.builder()
                .id(1L).doctor(doctor).patient(patient).build();

        SickLeaveRequest request = new SickLeaveRequest();
        request.setExaminationId(1L);
        request.setStartDate(LocalDate.now().minusDays(3)); // 3 дни назад — невалидно
        request.setNumberOfDays(5);

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(examination));
        when(doctorRepository.findByUser_Username("doctor@test.com")).thenReturn(Optional.of(doctor));

        assertThrows(IllegalArgumentException.class,
                () -> sickLeaveService.createSickLeave(request, "doctor@test.com"),
                "Начална дата повече от 2 дни назад трябва да хвърли изключение");
    }

    // Болничен без валиден преглед → ResourceNotFoundException
    @Test
    void createSickLeave_throwsResourceNotFound_whenExaminationNotFound() {
        SickLeaveRequest request = new SickLeaveRequest();
        request.setExaminationId(99L);
        request.setStartDate(LocalDate.now());
        request.setNumberOfDays(5);

        when(examinationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> sickLeaveService.createSickLeave(request, "doctor@test.com"),
                "Трябва да хвърли ResourceNotFoundException при несъществуващ преглед");
    }

    // Лекарят и пациентът се взимат автоматично от прегледа, не от заявката
    @Test
    void createSickLeave_autoFillsDoctorAndPatientFromExamination() {
        Doctor doctor = Doctor.builder().id(5L).firstName("Мария").lastName("Иванова").build();
        Patient patient = Patient.builder().id(7L).firstName("Елена").lastName("Тодорова").build();
        Examination examination = Examination.builder()
                .id(1L).doctor(doctor).patient(patient).build();

        SickLeaveRequest request = new SickLeaveRequest();
        request.setExaminationId(1L);
        request.setStartDate(LocalDate.now()); // днес — валидно
        request.setNumberOfDays(5);

        SickLeave savedSickLeave = SickLeave.builder()
                .id(1L).doctor(doctor).patient(patient).examination(examination)
                .startDate(LocalDate.now()).numberOfDays(5).build();

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(examination));
        when(doctorRepository.findByUser_Username("doctor@test.com")).thenReturn(Optional.of(doctor));
        when(sickLeaveRepository.save(any(SickLeave.class))).thenReturn(savedSickLeave);
        when(sickLeaveMapper.toResponse(any())).thenReturn(null);

        sickLeaveService.createSickLeave(request, "doctor@test.com");

        // Хващаме обекта изпратен за запис
        ArgumentCaptor<SickLeave> captor = ArgumentCaptor.forClass(SickLeave.class);
        verify(sickLeaveRepository).save(captor.capture());

        SickLeave captured = captor.getValue();
        // Лекарят трябва да е от прегледа (id=5), не от заявката
        assertEquals(5L, captured.getDoctor().getId(),
                "Лекарят трябва да се взима автоматично от прегледа");
        // Пациентът също трябва да е от прегледа (id=7)
        assertEquals(7L, captured.getPatient().getId(),
                "Пациентът трябва да се взима автоматично от прегледа");
    }

    // Начална дата точно преди 2 дни е валидна (граничен случай)
    @Test
    void createSickLeave_succeedsWhenStartDateExactly2DaysAgo() {
        Doctor doctor = Doctor.builder().id(1L).build();
        Patient patient = Patient.builder().id(1L).build();
        Examination examination = Examination.builder()
                .id(1L).doctor(doctor).patient(patient).build();

        SickLeaveRequest request = new SickLeaveRequest();
        request.setExaminationId(1L);
        request.setStartDate(LocalDate.now().minusDays(2)); // точно 2 дни — валидно
        request.setNumberOfDays(3);

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(examination));
        when(doctorRepository.findByUser_Username("doctor@test.com")).thenReturn(Optional.of(doctor));
        when(sickLeaveRepository.save(any())).thenReturn(SickLeave.builder().id(1L)
                .doctor(doctor).patient(patient).examination(examination).build());
        when(sickLeaveMapper.toResponse(any())).thenReturn(null);

        // Не трябва да хвърля изключение
        assertDoesNotThrow(() -> sickLeaveService.createSickLeave(request, "doctor@test.com"));
    }

    // Лекарят не може да издава болничен лист за чужд преглед → AccessDeniedException
    @Test
    void createSickLeave_throwsAccessDenied_whenDoctorDoesNotOwnExamination() {
        Doctor ownerDoctor = Doctor.builder().id(1L).build();
        Doctor otherDoctor = Doctor.builder().id(2L).build();
        Patient patient = Patient.builder().id(1L).build();

        Examination examination = Examination.builder()
                .id(1L).doctor(ownerDoctor).patient(patient).build();

        SickLeaveRequest request = new SickLeaveRequest();
        request.setExaminationId(1L);
        request.setStartDate(LocalDate.now());
        request.setNumberOfDays(3);

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(examination));
        // Влезлият лекар е с id=2, но прегледът принадлежи на лекар с id=1
        when(doctorRepository.findByUser_Username("other@test.com")).thenReturn(Optional.of(otherDoctor));

        assertThrows(AccessDeniedException.class,
                () -> sickLeaveService.createSickLeave(request, "other@test.com"),
                "Лекарят не трябва да може да издава болничен лист за чужд преглед");
    }
}
