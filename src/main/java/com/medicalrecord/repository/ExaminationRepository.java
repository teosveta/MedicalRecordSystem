package com.medicalrecord.repository;

import com.medicalrecord.dto.statistics.DoctorCountResponse;
import com.medicalrecord.dto.statistics.IdCountRow;
import com.medicalrecord.dto.statistics.PaymentByDoctorResponse;
import com.medicalrecord.entity.Doctor;
import com.medicalrecord.entity.Examination;
import com.medicalrecord.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExaminationRepository extends JpaRepository<Examination, Long> {

    List<Examination> findByPatient(Patient patient);

    List<Examination> findByDoctor(Doctor doctor);

    List<Examination> findByDoctorAndExaminationDateBetween(Doctor doctor, LocalDate from, LocalDate to);

    @Query("SELECT e FROM Examination e WHERE e.diagnosis.id = :diagnosisId")
    List<Examination> findByDiagnosisId(@Param("diagnosisId") Long diagnosisId);

    // Брой прегледи по диагноза — сортирани низходящо (за статистика)
    @Query("SELECT new com.medicalrecord.dto.statistics.IdCountRow(e.diagnosis.id, COUNT(e)) " +
           "FROM Examination e GROUP BY e.diagnosis.id ORDER BY COUNT(e) DESC")
    List<IdCountRow> countByDiagnosis();

    // Брой прегледи по лекар
    @Query("SELECT new com.medicalrecord.dto.statistics.DoctorCountResponse(" +
           "e.doctor.id, CONCAT(e.doctor.firstName, ' ', e.doctor.lastName), COUNT(e)) " +
           "FROM Examination e " +
           "GROUP BY e.doctor.id, e.doctor.firstName, e.doctor.lastName " +
           "ORDER BY COUNT(e) DESC")
    List<DoctorCountResponse> countByDoctor();

    // Обща сума на платени прегледи от неосигурени пациенти
    @Query("SELECT SUM(e.price) FROM Examination e WHERE e.paidByPatient = true")
    BigDecimal sumPaidByPatient();

    // Всички суми по лекар: общо, от НЗОК (осигурени), от пациент (неосигурени)
    @Query("SELECT new com.medicalrecord.dto.statistics.PaymentByDoctorResponse(" +
           "e.doctor.id, CONCAT(e.doctor.firstName, ' ', e.doctor.lastName), " +
           "SUM(e.price), " +
           "SUM(CASE WHEN e.paidByPatient = false THEN e.price END), " +
           "SUM(CASE WHEN e.paidByPatient = true  THEN e.price END)) " +
           "FROM Examination e " +
           "GROUP BY e.doctor.id, e.doctor.firstName, e.doctor.lastName " +
           "ORDER BY SUM(e.price) DESC")
    List<PaymentByDoctorResponse> sumAllPaymentsByDoctor();
}
