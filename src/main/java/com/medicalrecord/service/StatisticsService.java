package com.medicalrecord.service;

import com.medicalrecord.dto.diagnosis.DiagnosisResponse;
import com.medicalrecord.dto.doctor.DoctorResponse;
import com.medicalrecord.dto.examination.ExaminationResponse;
import com.medicalrecord.dto.patient.PatientResponse;
import com.medicalrecord.dto.statistics.DoctorCountResponse;
import com.medicalrecord.dto.statistics.MonthStatisticsResponse;
import com.medicalrecord.dto.statistics.PaymentByDoctorResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface StatisticsService {

    List<PatientResponse> getPatientsByDiagnosis(Long diagnosisId);

    DiagnosisResponse getMostCommonDiagnosis();

    List<PatientResponse> getPatientsByDoctor(Long doctorId);

    BigDecimal getTotalPatientPayments();

    List<PaymentByDoctorResponse> getPatientPaymentsByDoctor();

    List<DoctorCountResponse> getPatientsCountPerGP();

    List<DoctorCountResponse> getVisitsPerDoctor();

    List<ExaminationResponse> getExaminationsByDoctorAndPeriod(Long doctorId, LocalDate from, LocalDate to);

    MonthStatisticsResponse getMonthWithMostSickLeaves();

    DoctorResponse getDoctorWithMostSickLeaves();

    // Статистики само за текущия лекар
    long getMyVisitsCount(String username);

    long getMyPatientsCount(String username);

    BigDecimal getMyTotalRevenue(String username);

    BigDecimal getMyPatientPayments(String username);

    DiagnosisResponse getMyMostCommonDiagnosis(String username);

    long getMySickLeavesCount(String username);

    List<ExaminationResponse> getMyExaminationsByPeriod(String username, LocalDate from, LocalDate to);

    List<PatientResponse> getMyPatientsByDiagnosis(String username, Long diagnosisId);
}
