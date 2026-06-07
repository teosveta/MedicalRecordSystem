package com.medicalrecord.repository;

import com.medicalrecord.dto.statistics.IdCountRow;
import com.medicalrecord.dto.statistics.MonthStatisticsResponse;
import com.medicalrecord.entity.Doctor;
import com.medicalrecord.entity.Examination;
import com.medicalrecord.entity.Patient;
import com.medicalrecord.entity.SickLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SickLeaveRepository extends JpaRepository<SickLeave, Long> {

    List<SickLeave> findByPatient(Patient patient);

    List<SickLeave> findByDoctor(Doctor doctor);

    boolean existsByExaminationId(Long examinationId);

    boolean existsByExamination(Examination examination);

    List<SickLeave> findAllByExamination(Examination examination);

    void deleteAllByExaminationIn(List<Examination> examinations);

    // Месецът с най-много болнични листове
    @Query("SELECT new com.medicalrecord.dto.statistics.MonthStatisticsResponse(" +
           "MONTH(s.startDate), YEAR(s.startDate), COUNT(s)) " +
           "FROM SickLeave s " +
           "GROUP BY YEAR(s.startDate), MONTH(s.startDate) " +
           "ORDER BY COUNT(s) DESC")
    List<MonthStatisticsResponse> findMonthWithMostSickLeaves();

    // Лекарят с най-много издадени болнични листове
    @Query("SELECT new com.medicalrecord.dto.statistics.IdCountRow(s.doctor.id, COUNT(s)) " +
           "FROM SickLeave s GROUP BY s.doctor.id ORDER BY COUNT(s) DESC")
    List<IdCountRow> countByDoctor();
}
