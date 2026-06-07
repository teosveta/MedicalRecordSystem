package com.medicalrecord.service.impl;

import com.medicalrecord.dto.diagnosis.DiagnosisResponse;
import com.medicalrecord.dto.doctor.DoctorResponse;
import com.medicalrecord.dto.examination.ExaminationResponse;
import com.medicalrecord.dto.patient.PatientResponse;
import com.medicalrecord.dto.statistics.DoctorCountResponse;
import com.medicalrecord.dto.statistics.IdCountRow;
import com.medicalrecord.dto.statistics.MonthStatisticsResponse;
import com.medicalrecord.dto.statistics.PaymentByDoctorResponse;
import com.medicalrecord.entity.Diagnosis;
import com.medicalrecord.entity.Doctor;
import com.medicalrecord.exception.ResourceNotFoundException;
import com.medicalrecord.mapper.DiagnosisMapper;
import com.medicalrecord.mapper.DoctorMapper;
import com.medicalrecord.mapper.ExaminationMapper;
import com.medicalrecord.mapper.PatientMapper;
import com.medicalrecord.repository.*;
import com.medicalrecord.service.StatisticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StatisticsServiceImpl implements StatisticsService {

    private final ExaminationRepository examinationRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final SickLeaveRepository sickLeaveRepository;
    private final PatientMapper patientMapper;
    private final DiagnosisMapper diagnosisMapper;
    private final DoctorMapper doctorMapper;
    private final ExaminationMapper examinationMapper;

    public StatisticsServiceImpl(ExaminationRepository examinationRepository,
                                 DiagnosisRepository diagnosisRepository,
                                 DoctorRepository doctorRepository,
                                 PatientRepository patientRepository,
                                 SickLeaveRepository sickLeaveRepository,
                                 PatientMapper patientMapper,
                                 DiagnosisMapper diagnosisMapper,
                                 DoctorMapper doctorMapper,
                                 ExaminationMapper examinationMapper) {
        this.examinationRepository = examinationRepository;
        this.diagnosisRepository = diagnosisRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.sickLeaveRepository = sickLeaveRepository;
        this.patientMapper = patientMapper;
        this.diagnosisMapper = diagnosisMapper;
        this.doctorMapper = doctorMapper;
        this.examinationMapper = examinationMapper;
    }

    @Override
    public List<PatientResponse> getPatientsByDiagnosis(Long diagnosisId) {
        // Проверяваме дали диагнозата съществува
        diagnosisRepository.findById(diagnosisId)
                .orElseThrow(() -> new ResourceNotFoundException("Диагноза", "id", diagnosisId));

        // Намираме всички уникални пациенти с тази диагноза
        return examinationRepository.findByDiagnosisId(diagnosisId).stream()
                .map(e -> e.getPatient())
                .distinct()
                .map(patientMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DiagnosisResponse getMostCommonDiagnosis() {
        List<IdCountRow> results = examinationRepository.countByDiagnosis();
        if (results.isEmpty()) {
            throw new ResourceNotFoundException("Диагноза", "брой прегледи", "0");
        }
        Long diagnosisId = results.get(0).id();
        Diagnosis diagnosis = diagnosisRepository.findById(diagnosisId)
                .orElseThrow(() -> new ResourceNotFoundException("Диагноза", "id", diagnosisId));
        return diagnosisMapper.toResponse(diagnosis);
    }

    @Override
    public List<PatientResponse> getPatientsByDoctor(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Лекар", "id", doctorId));
        return patientRepository.findByPersonalDoctor(doctor).stream()
                .map(patientMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal getTotalPatientPayments() {
        BigDecimal total = examinationRepository.sumPaidByPatient();
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public List<PaymentByDoctorResponse> getPatientPaymentsByDoctor() {
        return examinationRepository.sumAllPaymentsByDoctor();
    }

    @Override
    public List<DoctorCountResponse> getPatientsCountPerGP() {
        return patientRepository.countPatientsByPersonalDoctor();
    }

    @Override
    public List<DoctorCountResponse> getVisitsPerDoctor() {
        return examinationRepository.countByDoctor();
    }

    @Override
    public List<ExaminationResponse> getExaminationsByDoctorAndPeriod(
            Long doctorId, LocalDate from, LocalDate to) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Лекар", "id", doctorId));
        return examinationRepository.findByDoctorAndExaminationDateBetween(doctor, from, to).stream()
                .map(examinationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public MonthStatisticsResponse getMonthWithMostSickLeaves() {
        List<MonthStatisticsResponse> results = sickLeaveRepository.findMonthWithMostSickLeaves();
        if (results.isEmpty()) {
            throw new ResourceNotFoundException("Болничен лист", "брой", "0");
        }
        return results.get(0);
    }

    @Override
    public DoctorResponse getDoctorWithMostSickLeaves() {
        List<IdCountRow> results = sickLeaveRepository.countByDoctor();
        if (results.isEmpty()) {
            throw new ResourceNotFoundException("Лекар", "болнични листове", "0");
        }
        Long doctorId = results.get(0).id();
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Лекар", "id", doctorId));
        return doctorMapper.toResponse(doctor);
    }

    // Помощен метод — намиране на лекар по потребителско име
    private Doctor findDoctorByUsername(String username) {
        return doctorRepository.findByUser_Username(username)
                .orElseThrow(() -> new ResourceNotFoundException("Лекар", "потребителско име", username));
    }

    @Override
    public long getMyVisitsCount(String username) {
        Doctor doctor = findDoctorByUsername(username);
        return examinationRepository.findByDoctor(doctor).size();
    }

    @Override
    public long getMyPatientsCount(String username) {
        Doctor doctor = findDoctorByUsername(username);
        return examinationRepository.findByDoctor(doctor).stream()
                .map(e -> e.getPatient().getId())
                .distinct()
                .count();
    }

    @Override
    public BigDecimal getMyTotalRevenue(String username) {
        Doctor doctor = findDoctorByUsername(username);
        return examinationRepository.findByDoctor(doctor).stream()
                .map(e -> e.getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal getMyPatientPayments(String username) {
        Doctor doctor = findDoctorByUsername(username);
        return examinationRepository.findByDoctor(doctor).stream()
                .filter(e -> e.isPaidByPatient())
                .map(e -> e.getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public DiagnosisResponse getMyMostCommonDiagnosis(String username) {
        Doctor doctor = findDoctorByUsername(username);
        var exams = examinationRepository.findByDoctor(doctor);
        if (exams.isEmpty()) {
            throw new ResourceNotFoundException("Диагноза", "прегледи", "0");
        }
        Map<Long, Long> counts = exams.stream()
                .collect(Collectors.groupingBy(e -> e.getDiagnosis().getId(), Collectors.counting()));
        Long topDiagId = counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseThrow();
        Diagnosis diagnosis = diagnosisRepository.findById(topDiagId)
                .orElseThrow(() -> new ResourceNotFoundException("Диагноза", "id", topDiagId));
        return diagnosisMapper.toResponse(diagnosis);
    }

    @Override
    public long getMySickLeavesCount(String username) {
        Doctor doctor = findDoctorByUsername(username);
        return sickLeaveRepository.findByDoctor(doctor).size();
    }

    @Override
    public List<ExaminationResponse> getMyExaminationsByPeriod(String username, LocalDate from, LocalDate to) {
        Doctor doctor = findDoctorByUsername(username);
        return examinationRepository.findByDoctorAndExaminationDateBetween(doctor, from, to).stream()
                .map(examinationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PatientResponse> getMyPatientsByDiagnosis(String username, Long diagnosisId) {
        diagnosisRepository.findById(diagnosisId)
                .orElseThrow(() -> new ResourceNotFoundException("Диагноза", "id", diagnosisId));
        Doctor doctor = findDoctorByUsername(username);
        return examinationRepository.findByDoctor(doctor).stream()
                .filter(e -> e.getDiagnosis().getId().equals(diagnosisId))
                .map(e -> e.getPatient())
                .distinct()
                .map(patientMapper::toResponse)
                .collect(Collectors.toList());
    }
}
