package com.medicalrecord.service.impl;

import com.medicalrecord.dto.doctor.ChangePasswordRequest;
import com.medicalrecord.dto.doctor.DoctorRequest;
import com.medicalrecord.dto.doctor.DoctorResponse;
import com.medicalrecord.dto.doctor.DoctorUpdateRequest;
import com.medicalrecord.dto.patient.PatientResponse;
import com.medicalrecord.entity.Doctor;
import com.medicalrecord.entity.Examination;
import com.medicalrecord.entity.User;
import com.medicalrecord.enums.Role;
import com.medicalrecord.exception.ResourceNotFoundException;
import com.medicalrecord.mapper.DoctorMapper;
import com.medicalrecord.mapper.PatientMapper;
import com.medicalrecord.repository.*;
import com.medicalrecord.service.DoctorService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final ExaminationRepository examinationRepository;
    private final SickLeaveRepository sickLeaveRepository;
    private final DoctorMapper doctorMapper;
    private final PatientMapper patientMapper;
    private final PasswordEncoder passwordEncoder;

    public DoctorServiceImpl(DoctorRepository doctorRepository,
                             UserRepository userRepository,
                             PatientRepository patientRepository,
                             ExaminationRepository examinationRepository,
                             SickLeaveRepository sickLeaveRepository,
                             DoctorMapper doctorMapper,
                             PatientMapper patientMapper,
                             PasswordEncoder passwordEncoder) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.examinationRepository = examinationRepository;
        this.sickLeaveRepository = sickLeaveRepository;
        this.doctorMapper = doctorMapper;
        this.patientMapper = patientMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorResponse> getAllDoctors() {
        return doctorRepository.findAll().stream()
                .map(doctorMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DoctorResponse updateDoctor(Long id, DoctorUpdateRequest request) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Лекар", "id", id));

        if (!doctor.getUniqueIdentificationNumber().equals(request.getUniqueIdentificationNumber())
                && doctorRepository.existsByUniqueIdentificationNumber(request.getUniqueIdentificationNumber())) {
            throw new IllegalArgumentException(
                    "Лекар с идентификационен номер '" + request.getUniqueIdentificationNumber() + "' вече съществува");
        }

        doctor.setFirstName(request.getFirstName());
        doctor.setLastName(request.getLastName());
        doctor.setUniqueIdentificationNumber(request.getUniqueIdentificationNumber());
        doctor.getSpecialties().clear();
        doctor.getSpecialties().addAll(request.getSpecialties());
        doctor.setCanBeGP(request.isCanBeGP());

        return doctorMapper.toResponse(doctorRepository.save(doctor));
    }

    @Override
    public DoctorResponse createDoctor(DoctorRequest request) {
        // Проверяваме за дублиране на имейл
        if (userRepository.existsByUsername(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Потребител с имейл '" + request.getEmail() + "' вече съществува");
        }
        // Проверяваме за дублиране на идентификационен номер
        if (doctorRepository.existsByUniqueIdentificationNumber(request.getUniqueIdentificationNumber())) {
            throw new IllegalArgumentException(
                    "Лекар с идентификационен номер '" + request.getUniqueIdentificationNumber() + "' вече съществува");
        }

        User user = User.builder()
                .username(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.DOCTOR)
                .enabled(true)
                .build();
        userRepository.save(user);

        Doctor doctor = Doctor.builder()
                .uniqueIdentificationNumber(request.getUniqueIdentificationNumber())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .specialties(new HashSet<>(request.getSpecialties()))
                .canBeGP(request.isCanBeGP())
                .user(user)
                .build();

        return doctorMapper.toResponse(doctorRepository.save(doctor));
    }

    @Override
    public void deleteDoctor(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Лекар", "id", id));

        // Освобождаваме пациентите от техния личен лекар
        patientRepository.findByPersonalDoctor(doctor).forEach(patient -> {
            patient.setPersonalDoctor(null);
            patientRepository.save(patient);
        });

        // Изтриваме болничните листове и прегледите на лекаря
        List<Examination> examinations = examinationRepository.findByDoctor(doctor);
        sickLeaveRepository.deleteAllByExaminationIn(examinations);
        examinationRepository.deleteAll(examinations);

        User user = doctor.getUser();
        doctorRepository.delete(doctor);
        if (user != null) {
            userRepository.delete(user);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse getDoctorByUsername(String username) {
        Doctor doctor = doctorRepository.findByUser_Username(username)
                .orElseThrow(() -> new ResourceNotFoundException("Лекар", "username", username));
        return doctorMapper.toResponse(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientResponse> getMyPatients(String username) {
        Doctor doctor = doctorRepository.findByUser_Username(username)
                .orElseThrow(() -> new ResourceNotFoundException("Лекар", "username", username));
        return patientRepository.findByPersonalDoctor(doctor).stream()
                .map(patientMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorResponse> getGpDoctors() {
        return doctorRepository.findByCanBeGPTrue().stream()
                .map(doctorMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Потребител", "username", username));

        // Проверяваме дали текущата парола е правилна
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Текущата парола е неправилна");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
