package com.medicalrecord.service.impl;

import com.medicalrecord.dto.auth.LoginRequest;
import com.medicalrecord.dto.auth.LoginResponse;
import com.medicalrecord.dto.auth.RegisterRequest;
import com.medicalrecord.entity.Doctor;
import com.medicalrecord.entity.Patient;
import com.medicalrecord.entity.User;
import com.medicalrecord.enums.Role;
import com.medicalrecord.repository.DoctorRepository;
import com.medicalrecord.repository.PatientRepository;
import com.medicalrecord.repository.UserRepository;
import com.medicalrecord.security.JwtUtil;
import com.medicalrecord.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserRepository userRepository,
                           PatientRepository patientRepository,
                           DoctorRepository doctorRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Невалидни потребителско име или парола");
        }

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Потребителят не е намерен"));

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return new LoginResponse(token, user.getUsername(), user.getRole().name());
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException(
                    "Потребител с имейл '" + request.getUsername() + "' вече съществува");
        }
        if (patientRepository.existsByEgn(request.getEgn())) {
            throw new IllegalArgumentException(
                    "Пациент с ЕГН '" + request.getEgn() + "' вече е регистриран");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.PATIENT)
                .enabled(true)
                .build();
        userRepository.save(user);

        Doctor personalDoctor = null;
        if (request.getPersonalDoctorId() != null) {
            personalDoctor = doctorRepository.findById(request.getPersonalDoctorId()).orElse(null);
        }

        Patient patient = Patient.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .egn(request.getEgn())
                .personalDoctor(personalDoctor)
                .healthInsured(false)
                .user(user)
                .build();
        patientRepository.save(patient);
    }
}
