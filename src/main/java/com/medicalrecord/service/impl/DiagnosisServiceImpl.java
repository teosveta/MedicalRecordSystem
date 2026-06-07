package com.medicalrecord.service.impl;

import com.medicalrecord.dto.diagnosis.DiagnosisRequest;
import com.medicalrecord.dto.diagnosis.DiagnosisResponse;
import com.medicalrecord.entity.Diagnosis;
import com.medicalrecord.enums.Specialty;
import com.medicalrecord.exception.ResourceNotFoundException;
import com.medicalrecord.mapper.DiagnosisMapper;
import com.medicalrecord.repository.DiagnosisRepository;
import com.medicalrecord.service.DiagnosisService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class DiagnosisServiceImpl implements DiagnosisService {

    private final DiagnosisRepository diagnosisRepository;
    private final DiagnosisMapper diagnosisMapper;

    public DiagnosisServiceImpl(DiagnosisRepository diagnosisRepository,
                                DiagnosisMapper diagnosisMapper) {
        this.diagnosisRepository = diagnosisRepository;
        this.diagnosisMapper = diagnosisMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiagnosisResponse> getAllDiagnoses() {
        return diagnosisRepository.findAll().stream()
                .map(diagnosisMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiagnosisResponse> getDiagnosesBySpecialties(Set<Specialty> specialties) {
        // ОПЛ е общопрактикуващ — може да използва всяка диагноза
        if (specialties.contains(Specialty.GP)) {
            return diagnosisRepository.findAll().stream()
                    .map(diagnosisMapper::toResponse)
                    .collect(Collectors.toList());
        }

        // Обединяваме диагнозите за всички специалности на лекаря (без дублирания)
        Set<Diagnosis> diagnosisSet = new LinkedHashSet<>();
        for (Specialty specialty : specialties) {
            diagnosisSet.addAll(diagnosisRepository.findBySpecialtiesContaining(specialty));
        }
        List<Diagnosis> diagnoses = new ArrayList<>(diagnosisSet);
        // Добавяме Z00 ако не е включена (видима за всички специалности)
        boolean hasZ00 = diagnoses.stream().anyMatch(d -> "Z00".equals(d.getCode()));
        if (!hasZ00) {
            diagnosisRepository.findByCode("Z00").ifPresent(diagnoses::add);
        }
        return diagnoses.stream()
                .map(diagnosisMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DiagnosisResponse createDiagnosis(DiagnosisRequest request) {
        if (diagnosisRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException(
                    "Диагноза с код '" + request.getCode() + "' вече съществува");
        }

        Diagnosis diagnosis = Diagnosis.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .build();

        return diagnosisMapper.toResponse(diagnosisRepository.save(diagnosis));
    }

    @Override
    public DiagnosisResponse updateDiagnosis(Long id, DiagnosisRequest request) {
        Diagnosis diagnosis = diagnosisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Диагноза", "id", id));

        // Проверяваме за дублиране на код само ако е различен от текущия
        if (!diagnosis.getCode().equals(request.getCode()) &&
                diagnosisRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException(
                    "Диагноза с код '" + request.getCode() + "' вече съществува");
        }

        diagnosis.setCode(request.getCode());
        diagnosis.setName(request.getName());
        diagnosis.setDescription(request.getDescription());

        return diagnosisMapper.toResponse(diagnosisRepository.save(diagnosis));
    }

    @Override
    public void deleteDiagnosis(Long id) {
        Diagnosis diagnosis = diagnosisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Диагноза", "id", id));
        diagnosisRepository.delete(diagnosis);
    }
}
