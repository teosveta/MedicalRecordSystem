package com.medicalrecord.service;

import com.medicalrecord.dto.doctor.ChangePasswordRequest;
import com.medicalrecord.dto.doctor.DoctorRequest;
import com.medicalrecord.dto.doctor.DoctorResponse;
import com.medicalrecord.dto.doctor.DoctorUpdateRequest;
import com.medicalrecord.dto.patient.PatientResponse;

import java.util.List;

public interface DoctorService {

    List<DoctorResponse> getAllDoctors();

    DoctorResponse createDoctor(DoctorRequest request);

    DoctorResponse updateDoctor(Long id, DoctorUpdateRequest request);

    void deleteDoctor(Long id);

    void changePassword(String username, ChangePasswordRequest request);

    DoctorResponse getDoctorByUsername(String username);

    List<DoctorResponse> getGpDoctors();

    // Пациентите на текущия лекар (personalDoctor = me)
    List<PatientResponse> getMyPatients(String username);
}
