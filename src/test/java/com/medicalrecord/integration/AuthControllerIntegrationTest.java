package com.medicalrecord.integration;

import com.medicalrecord.repository.PatientRepository;
import com.medicalrecord.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционни тестове за AuthController.
 * Тества реалния поток: HTTP заявка → контролер → услуга → база данни.
 *
 * Начални данни (от DataInitializer):
 *  - admin@medical.com / Az1234! (ADMIN)
 *  - d.petrov@medical.com / Az1234! (DOCTOR)
 *  - p.kolev@medical.com / Az1234! (PATIENT)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    // Имейл на тест потребител, създаван само в тестове — изтрива се в @AfterEach
    private static final String TEST_EMAIL = "test.register@integration.com";

    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;

    @AfterEach
    void cleanup() {
        // Изтриваме само данните, създадени от тестовете — не засягаме seed данните
        patientRepository.findByUser_Username(TEST_EMAIL)
                .ifPresent(patientRepository::delete);
        userRepository.findByUsername(TEST_EMAIL)
                .ifPresent(userRepository::delete);
    }

    // ─────────────────────────────────────────────────
    //  ТЕСТОВЕ ЗА LOGIN
    // ─────────────────────────────────────────────────

    @Test
    void login_validAdminCredentials_returns200WithTokenAndRole() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin@medical.com","password":"Az1234!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("admin@medical.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void login_validDoctorCredentials_returnsDoctorRole() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"d.petrov@medical.com","password":"Az1234!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("DOCTOR"));
    }

    @Test
    void login_validPatientCredentials_returnsPatientRole() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"p.kolev@medical.com","password":"Az1234!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PATIENT"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin@medical.com","password":"ГРЕШНА_ПАРОЛА"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_nonExistentUser_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"nobody@test.com","password":"Az1234!"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // @Email валидацията на LoginRequest трябва да отхвърли невалиден формат
    @Test
    void login_invalidEmailFormat_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"неимейл","password":"Az1234!"}
                                """))
                .andExpect(status().isBadRequest());
    }

    // @NotBlank валидацията трябва да отхвърли празна парола
    @Test
    void login_blankPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin@medical.com","password":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────
    //  ТЕСТОВЕ ЗА REGISTER
    // ─────────────────────────────────────────────────

    @Test
    void register_validNewPatient_returns201() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "firstName": "Тест",
                                  "lastName":  "Тестов",
                                  "egn":       "9501019876",
                                  "username":  "%s",
                                  "password":  "Test123!"
                                }
                                """, TEST_EMAIL)))
                .andExpect(status().isCreated());
    }

    // Дублиран имейл → IllegalArgumentException → 400
    @Test
    void register_duplicateEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Тест",
                                  "lastName":  "Тестов",
                                  "egn":       "9012340001",
                                  "username":  "admin@medical.com",
                                  "password":  "Test123!"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    // @Size(min=7) на паролата трябва да отхвърли кратка парола
    @Test
    void register_passwordTooShort_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Тест",
                                  "lastName":  "Тестов",
                                  "egn":       "9012340002",
                                  "username":  "short@test.com",
                                  "password":  "Ab1"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    // @Pattern на ЕГН трябва да отхвърли невалиден формат (не е точно 10 цифри)
    @Test
    void register_invalidEgn_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Тест",
                                  "lastName":  "Тестов",
                                  "egn":       "123",
                                  "username":  "egn@test.com",
                                  "password":  "Test123!"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
