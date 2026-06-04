package com.medicalrecord.repository;

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
    @Query("SELECT e.diagnosis.id, COUNT(e) FROM Examination e GROUP BY e.diagnosis.id ORDER BY COUNT(e) DESC")
    List<Object[]> countByDiagnosis();

    // Брой прегледи по лекар
    @Query("SELECT e.doctor.id, COUNT(e) FROM Examination e GROUP BY e.doctor.id ORDER BY COUNT(e) DESC")
    List<Object[]> countByDoctor();

    // Обща сума на платени прегледи от неосигурени пациенти
    @Query("SELECT SUM(e.price) FROM Examination e WHERE e.paidByPatient = true")
    BigDecimal sumPaidByPatient();

    // Платени суми по лекар
    @Query("SELECT e.doctor.id, SUM(e.price) FROM Examination e WHERE e.paidByPatient = true GROUP BY e.doctor.id")
    List<Object[]> sumPaymentsByDoctor();
}
