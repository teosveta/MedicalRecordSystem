package com.medicalrecord.integration;

import com.medicalrecord.entity.Diagnosis;
import com.medicalrecord.entity.Doctor;
import com.medicalrecord.entity.Examination;
import com.medicalrecord.entity.Patient;
import com.medicalrecord.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционни тестове за ExaminationController.
 *
 * Тества ролева видимост, създаване, изтриване и правилото:
 * "лекарят не може да изтрива чужди прегледи или прегледи от минали дни".
 *
 * Seed данни:
 *  - d.petrov@medical.com    — GP лекар с пациенти Колев, Тодорова, Стоянов
 *  - d.ivanova@medical.com   — кардиолог с пациент Димитрова
 *  - p.kolev@medical.com     — осигурен пациент (paidByPatient = false)
 *  - p.todorova@medical.com  — неосигурен пациент (paidByPatient = true)
 *  - 4 прегледа от 2025 г., 2 с болнични листове
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ExaminationControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private ExaminationRepository examinationRepository;
    @Autowired private PatientRepository      patientRepository;
    @Autowired private DiagnosisRepository    diagnosisRepository;
    @Autowired private DoctorRepository       doctorRepository;
    @Autowired private SickLeaveRepository    sickLeaveRepository;

    // Seed обекти — зареждат се от базата в @BeforeEach
    private Doctor  doctorPetrov;
    private Patient patientKolev;
    private Patient patientTodorova;
    private Diagnosis diagZ00;

    // ID на прегледа, създаден от тест — изтрива се в @AfterEach
    private Long createdExamId;

    @BeforeEach
    void loadSeedData() {
        doctorPetrov     = doctorRepository.findByUser_Username("d.petrov@medical.com").orElseThrow();
        patientKolev     = patientRepository.findByUser_Username("p.kolev@medical.com").orElseThrow();
        patientTodorova  = patientRepository.findByUser_Username("p.todorova@medical.com").orElseThrow();
        diagZ00          = diagnosisRepository.findByCode("Z00").orElseThrow();
        createdExamId    = null;
    }

    @AfterEach
    void cleanup() {
        if (createdExamId != null) {
            examinationRepository.findById(createdExamId).ifPresent(exam -> {
                sickLeaveRepository.deleteAllByExaminationIn(List.of(exam));
                examinationRepository.delete(exam);
            });
        }
    }

    // ─────────────────────────────────────────────────
    //  GET /api/examinations — ролева видимост
    // ─────────────────────────────────────────────────

    // DOCTOR вижда всички прегледи в системата (4 от seed данните)
    @Test
    void getExaminations_asDoctor_returns200WithAllExaminations() throws Exception {
        String token = getToken("d.petrov@medical.com", "Az1234!");

        mockMvc.perform(get("/api/examinations")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(4))));
    }

    // PATIENT (Колев) вижда само своите прегледи — само с patientName съдържащо "Колев"
    @Test
    void getExaminations_asPatient_returnsOnlyOwnExaminations() throws Exception {
        String token = getToken("p.kolev@medical.com", "Az1234!");

        mockMvc.perform(get("/api/examinations")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].patientName", everyItem(containsString("Колев"))));
    }

    // ADMIN вижда всички прегледи
    @Test
    void getExaminations_asAdmin_returns200WithAllExaminations() throws Exception {
        String token = getToken("admin@medical.com", "Az1234!");

        mockMvc.perform(get("/api/examinations")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(4))));
    }

    // Заявка без JWT токен → 401 Unauthorized
    @Test
    void getExaminations_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/examinations"))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────
    //  POST /api/examinations — създаване
    // ─────────────────────────────────────────────────

    // DOCTOR успешно създава преглед с днешна дата за осигурен пациент
    @Test
    void createExamination_asDoctorTodayDateInsuredPatient_returns201AndPaidByPatientFalse()
            throws Exception {
        String token = getToken("d.petrov@medical.com", "Az1234!");

        String body = String.format("""
                {
                  "examinationDate": "%s",
                  "patientId":       %d,
                  "diagnosisId":     %d,
                  "treatment":       "Предписан почивен режим и обилен прием на течности",
                  "price":           20.00
                }
                """, LocalDate.now(), patientKolev.getId(), diagZ00.getId());

        String response = mockMvc.perform(post("/api/examinations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                // Колев е осигурен → paidByPatient трябва да е false
                .andExpect(jsonPath("$.paidByPatient").value(false))
                .andExpect(jsonPath("$.doctorName").value(containsString("Петров")))
                .andReturn().getResponse().getContentAsString();

        // Запазваме ID за изчистване в @AfterEach
        createdExamId = objectMapper.readTree(response).get("id").asLong();
    }

    // DOCTOR създава преглед за неосигурен пациент → paidByPatient = true
    @Test
    void createExamination_forUninsuredPatient_returns201AndPaidByPatientTrue()
            throws Exception {
        String token = getToken("d.petrov@medical.com", "Az1234!");

        String body = String.format("""
                {
                  "examinationDate": "%s",
                  "patientId":       %d,
                  "diagnosisId":     %d,
                  "treatment":       "Предписана диета и медикаменти",
                  "price":           30.00
                }
                """, LocalDate.now(), patientTodorova.getId(), diagZ00.getId());

        String response = mockMvc.perform(post("/api/examinations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content(body))
                .andExpect(status().isCreated())
                // Тодорова е неосигурена → пациентът плаща сам
                .andExpect(jsonPath("$.paidByPatient").value(true))
                .andReturn().getResponse().getContentAsString();

        createdExamId = objectMapper.readTree(response).get("id").asLong();
    }

    // @PastOrPresent — дата от утре → 400 (Bean Validation)
    @Test
    void createExamination_withFutureDate_returns400() throws Exception {
        String token = getToken("d.petrov@medical.com", "Az1234!");

        String body = String.format("""
                {
                  "examinationDate": "%s",
                  "patientId":       %d,
                  "diagnosisId":     %d,
                  "treatment":       "Тест",
                  "price":           20.00
                }
                """, LocalDate.now().plusDays(1), patientKolev.getId(), diagZ00.getId());

        mockMvc.perform(post("/api/examinations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // Бизнес правило: дата повече от 2 дни назад → 400 (проверка в ExaminationServiceImpl)
    @Test
    void createExamination_withDateMoreThan2DaysAgo_returns400() throws Exception {
        String token = getToken("d.petrov@medical.com", "Az1234!");

        String body = String.format("""
                {
                  "examinationDate": "%s",
                  "patientId":       %d,
                  "diagnosisId":     %d,
                  "treatment":       "Тест",
                  "price":           20.00
                }
                """, LocalDate.now().minusDays(3), patientKolev.getId(), diagZ00.getId());

        mockMvc.perform(post("/api/examinations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // PATIENT не може да създава прегледи → @PreAuthorize("hasRole('DOCTOR')") → 403
    @Test
    void createExamination_asPatient_returns403() throws Exception {
        String token = getToken("p.kolev@medical.com", "Az1234!");

        String body = String.format("""
                {
                  "examinationDate": "%s",
                  "patientId":       %d,
                  "diagnosisId":     %d,
                  "treatment":       "Тест",
                  "price":           20.00
                }
                """, LocalDate.now(), patientKolev.getId(), diagZ00.getId());

        mockMvc.perform(post("/api/examinations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content(body))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────────
    //  DELETE /api/examinations/{id} — изтриване
    // ─────────────────────────────────────────────────

    // DOCTOR успешно изтрива собствен преглед от ДНЕС → 204
    @Test
    void deleteExamination_asDoctorForTodaysOwnExam_returns204() throws Exception {
        // Създаваме преглед от днес директно в базата (не чрез HTTP)
        Examination todayExam = examinationRepository.save(Examination.builder()
                .examinationDate(LocalDate.now())
                .doctor(doctorPetrov)
                .patient(patientKolev)
                .diagnosis(diagZ00)
                .treatment("Преглед за изтриване — интеграционен тест")
                .price(new BigDecimal("20.00"))
                .paidByPatient(false)
                .build());

        String token = getToken("d.petrov@medical.com", "Az1234!");

        mockMvc.perform(delete("/api/examinations/" + todayExam.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        // Прегледът е изтрит — @AfterEach няма какво да изчиства
    }

    // Не може да се изтрие преглед, към който е издаден болничен лист → 400
    @Test
    void deleteExamination_withExistingSickLeave_returns400() throws Exception {
        String token = getToken("admin@medical.com", "Az1234!");

        // Намираме първия преглед на Петров с издаден болничен лист
        Examination examWithSickLeave = examinationRepository.findByDoctor(doctorPetrov).stream()
                .filter(e -> sickLeaveRepository.existsByExaminationId(e.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Не е намерен преглед с болничен лист"));

        mockMvc.perform(delete("/api/examinations/" + examWithSickLeave.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }

    // DOCTOR не може да изтрие преглед от минали дни → 400
    @Test
    void deleteExamination_asDoctorForPastExam_returns400() throws Exception {
        // Seed прегледите на Петров имат болнични листове → не подхождат.
        // Създаваме минал преглед без болничен лист директно в базата.
        Examination pastExam = examinationRepository.save(Examination.builder()
                .examinationDate(LocalDate.of(2025, 6, 1))
                .doctor(doctorPetrov)
                .patient(patientKolev)
                .diagnosis(diagZ00)
                .treatment("Тест — минал преглед без болничен")
                .price(new BigDecimal("20.00"))
                .paidByPatient(false)
                .build());
        createdExamId = pastExam.getId();

        String doctorToken = getToken("d.petrov@medical.com", "Az1234!");

        mockMvc.perform(delete("/api/examinations/" + pastExam.getId())
                        .header("Authorization", bearer(doctorToken)))
                .andExpect(status().isBadRequest());
    }
}
