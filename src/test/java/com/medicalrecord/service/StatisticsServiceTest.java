package com.medicalrecord.service;

import com.medicalrecord.dto.diagnosis.DiagnosisResponse;
import com.medicalrecord.entity.Diagnosis;
import com.medicalrecord.exception.ResourceNotFoundException;
import com.medicalrecord.mapper.DiagnosisMapper;
import com.medicalrecord.mapper.DoctorMapper;
import com.medicalrecord.mapper.ExaminationMapper;
import com.medicalrecord.mapper.PatientMapper;
import com.medicalrecord.repository.*;
import com.medicalrecord.service.impl.StatisticsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock private ExaminationRepository examinationRepository;
    @Mock private DiagnosisRepository diagnosisRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private SickLeaveRepository sickLeaveRepository;
    @Mock private PatientMapper patientMapper;
    @Mock private DiagnosisMapper diagnosisMapper;
    @Mock private DoctorMapper doctorMapper;
    @Mock private ExaminationMapper examinationMapper;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    // Най-честата диагноза трябва да е тази с най-висок брой прегледи
    @Test
    void getMostCommonDiagnosis_returnsHighestExaminationCount() {
        Diagnosis grippe = Diagnosis.builder()
                .id(1L).code("J06").name("Остра инфекция на горните дихателни пътища").build();

        // Диагноза J06 има 5 прегледа, I10 има 3 — J06 трябва да е „най-честа"
        List<Object[]> countResults = List.of(
                new Object[]{1L, 5L},
                new Object[]{2L, 3L}
        );

        when(examinationRepository.countByDiagnosis()).thenReturn(countResults);
        when(diagnosisRepository.findById(1L)).thenReturn(Optional.of(grippe));

        DiagnosisResponse expectedResponse = new DiagnosisResponse();
        expectedResponse.setId(1L);
        expectedResponse.setCode("J06");
        when(diagnosisMapper.toResponse(grippe)).thenReturn(expectedResponse);

        DiagnosisResponse result = statisticsService.getMostCommonDiagnosis();

        assertNotNull(result);
        assertEquals(1L, result.getId(),
                "Трябва да върне диагнозата с id=1 (J06) като най-честа");
        assertEquals("J06", result.getCode());
        // Проверяваме, че diagnosisRepository е извикан с правилния id
        verify(diagnosisRepository).findById(1L);
    }

    // При липса на прегледи трябва да хвърли ResourceNotFoundException
    @Test
    void getMostCommonDiagnosis_throwsException_whenNoExaminations() {
        when(examinationRepository.countByDiagnosis()).thenReturn(Collections.emptyList());

        assertThrows(ResourceNotFoundException.class,
                () -> statisticsService.getMostCommonDiagnosis(),
                "Трябва да хвърли ResourceNotFoundException при липса на прегледи");
    }

    // Диагнозата с по-малко прегледи никога не трябва да е избрана
    @Test
    void getMostCommonDiagnosis_neverReturnsLessCommonDiagnosis() {
        Diagnosis commonDiagnosis = Diagnosis.builder().id(1L).code("J06").name("Грип").build();

        // Резултатите са наредени по брой DESC — първият е с най-висок
        List<Object[]> countResults = List.of(
                new Object[]{1L, 10L}, // J06 — 10 прегледа
                new Object[]{3L, 7L},  // I10 — 7 прегледа
                new Object[]{2L, 2L}   // E11 — 2 прегледа
        );

        when(examinationRepository.countByDiagnosis()).thenReturn(countResults);
        when(diagnosisRepository.findById(1L)).thenReturn(Optional.of(commonDiagnosis));

        DiagnosisResponse response = new DiagnosisResponse();
        response.setId(1L);
        when(diagnosisMapper.toResponse(commonDiagnosis)).thenReturn(response);

        DiagnosisResponse result = statisticsService.getMostCommonDiagnosis();

        // Проверяваме, че НЕ е върната диагноза с id=3 или id=2
        assertNotEquals(3L, result.getId(), "Не трябва да върне диагноза с id=3");
        assertNotEquals(2L, result.getId(), "Не трябва да върне диагноза с id=2");
        assertEquals(1L, result.getId(), "Трябва да върне диагноза с id=1 (най-много прегледи)");
    }
}
