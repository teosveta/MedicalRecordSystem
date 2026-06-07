package com.medicalrecord.integration;

import com.medicalrecord.entity.Diagnosis;
import com.medicalrecord.entity.Doctor;
import com.medicalrecord.entity.Examination;
import com.medicalrecord.entity.Patient;
import com.medicalrecord.entity.SickLeave;
import com.medicalrecord.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционни тестове за SickLeaveController.
 *
 * Проверява:
 *  - Ролевата видимост (ADMIN вижда всички, DOCTOR само свои, PATIENT само свои)
 *  - Издаване на болничен лист (включително DTO валидациите)
 *  - Забрана за издаване за чужд преглед
 *  - @NotTooOld валидацията при създаване и при актуализиране
 *
 * За тестовете за успешно издаване се създава свеж преглед от ДНЕС в @BeforeEach,
 * тъй като болничен лист не може да се издаде за преглед по-стар от 7 дни.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SickLeaveControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private SickLeaveRepository    sickLeaveRepository;
    @Autowired private ExaminationRepository  examinationRepository;
    @Autowired private DoctorRepository       doctorRepository;
    @Autowired private PatientRepository      patientRepository;
    @Autowired private DiagnosisRepository    diagnosisRepository;

    private Doctor    doctorPetrov;
    private Patient   patientStoyanov;  // осигурен пациент на Петров
    private Diagnosis diagZ00;

    // Свеж преглед от днес — създава се в @BeforeEach, изтрива се в @AfterEach
    private Examination freshExam;
    // ID на болничен лист, създаден от тест — изтрива се в @AfterEach
    private Long createdSickLeaveId;

    @BeforeEach
    void setup() {
        doctorPetrov    = doctorRepository.findByUser_Username("d.petrov@medical.com").orElseThrow();
        patientStoyanov = patientRepository.findByUser_Username("p.stoyanov@medical.com").orElseThrow();
        diagZ00         = diagnosisRepository.findByCode("Z00").orElseThrow();
        createdSickLeaveId = null;

        // Болничен лист може да се издаде само за преглед от последните 7 дни.
        // Seed прегледите са от 2025 г. → твърде стари.
        // Затова създаваме свеж преглед от днес директно в базата.
        freshExam = examinationRepository.save(Examination.builder()
                .examinationDate(LocalDate.now())
                .doctor(doctorPetrov)
                .patient(patientStoyanov)
                .diagnosis(diagZ00)
                .treatment("Тест преглед за болничен лист — интеграционен тест")
                .price(new BigDecimal("20.00"))
                .paidByPatient(false)
                .build());
    }

    @AfterEach
    void cleanup() {
        // Изтриваме болничния лист преди прегледа (FK constraint)
        if (createdSickLeaveId != null) {
            sickLeaveRepository.findById(createdSickLeaveId)
                    .ifPresent(sickLeaveRepository::delete);
        }
        examinationRepository.delete(freshExam);
    }

    // ─────────────────────────────────────────────────
    //  GET /api/sick-leaves — ролева видимост
    // ─────────────────────────────────────────────────

    // ADMIN вижда всички болнични листове (минимум 2 от seed данните)
    @Test
    void getSickLeaves_asAdmin_returns200WithAllLeaves() throws Exception {
        String token = getToken("admin@medical.com", "Az1234!");

        mockMvc.perform(get("/api/sick-leaves")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    // DOCTOR (Петров) вижда само свои болнични листове — doctorName съдържа "Петров"
    @Test
    void getSickLeaves_asDoctor_returnsOnlyOwnLeaves() throws Exception {
        String token = getToken("d.petrov@medical.com", "Az1234!");

        mockMvc.perform(get("/api/sick-leaves")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].doctorName", everyItem(containsString("Петров"))));
    }

    // PATIENT (Колев) вижда само своите болнични листове
    @Test
    void getSickLeaves_asPatient_returnsOnlyOwnLeaves() throws Exception {
        String token = getToken("p.kolev@medical.com", "Az1234!");

        mockMvc.perform(get("/api/sick-leaves")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].patientName", everyItem(containsString("Колев"))));
    }

    // ─────────────────────────────────────────────────
    //  POST /api/sick-leaves — създаване
    // ─────────────────────────────────────────────────

    // DOCTOR издава болничен лист за собствен преглед от днес → 201
    @Test
    void createSickLeave_asDoctorForOwnTodayExam_returns201() throws Exception {
        String token = getToken("d.petrov@medical.com", "Az1234!");

        String body = String.format("""
                {
                  "examinationId": %d,
                  "startDate":     "%s",
                  "numberOfDays":  5
                }
                """, freshExam.getId(), LocalDate.now());

        String response = mockMvc.perform(post("/api/sick-leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numberOfDays").value(5))
                .andExpect(jsonPath("$.doctorName").value(containsString("Петров")))
                .andExpect(jsonPath("$.patientName").value(containsString("Стоянов")))
                .andReturn().getResponse().getContentAsString();

        createdSickLeaveId = objectMapper.readTree(response).get("id").asLong();
    }

    // @NotTooOld — начална дата 3 дни назад → 400 (Bean Validation на DTO ниво)
    @Test
    void createSickLeave_withStartDate3DaysAgo_returns400() throws Exception {
        String token = getToken("d.petrov@medical.com", "Az1234!");

        String body = String.format("""
                {
                  "examinationId": %d,
                  "startDate":     "%s",
                  "numberOfDays":  3
                }
                """, freshExam.getId(), LocalDate.now().minusDays(3));

        mockMvc.perform(post("/api/sick-leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // @Max(30) — повече от 30 дни → 400 (Bean Validation на DTO ниво)
    @Test
    void createSickLeave_withMoreThan30Days_returns400() throws Exception {
        String token = getToken("d.petrov@medical.com", "Az1234!");

        String body = String.format("""
                {
                  "examinationId": %d,
                  "startDate":     "%s",
                  "numberOfDays":  31
                }
                """, freshExam.getId(), LocalDate.now());

        mockMvc.perform(post("/api/sick-leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // @Min(1) — 0 дни → 400
    @Test
    void createSickLeave_withZeroDays_returns400() throws Exception {
        String token = getToken("d.petrov@medical.com", "Az1234!");

        String body = String.format("""
                {
                  "examinationId": %d,
                  "startDate":     "%s",
                  "numberOfDays":  0
                }
                """, freshExam.getId(), LocalDate.now());

        mockMvc.perform(post("/api/sick-leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // Ивaнова се опитва да издаде болничен за преглед на Петров → 403
    @Test
    void createSickLeave_forAnotherDoctorsExam_returns403() throws Exception {
        // freshExam принадлежи на Петров — Иванова няма право
        String token = getToken("d.ivanova@medical.com", "Az1234!");

        String body = String.format("""
                {
                  "examinationId": %d,
                  "startDate":     "%s",
                  "numberOfDays":  3
                }
                """, freshExam.getId(), LocalDate.now());

        mockMvc.perform(post("/api/sick-leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content(body))
                .andExpect(status().isForbidden());
    }

    // PATIENT не може да издава болнични листове → @PreAuthorize("hasRole('DOCTOR')") → 403
    @Test
    void createSickLeave_asPatient_returns403() throws Exception {
        String token = getToken("p.kolev@medical.com", "Az1234!");

        String body = String.format("""
                {
                  "examinationId": %d,
                  "startDate":     "%s",
                  "numberOfDays":  3
                }
                """, freshExam.getId(), LocalDate.now());

        mockMvc.perform(post("/api/sick-leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content(body))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────────
    //  PUT /api/sick-leaves/{id} — актуализиране
    // ─────────────────────────────────────────────────

    // @NotTooOld на UpdateSickLeaveRequest — актуализиране с дата 3 дни назад → 400
    @Test
    void updateSickLeave_withOldStartDate_returns400() throws Exception {
        // Създаваме болничен лист в базата за да има какво да актуализираме
        SickLeave sickLeave = sickLeaveRepository.save(
                SickLeave.builder()
                        .examination(freshExam)
                        .startDate(LocalDate.now())
                        .numberOfDays(3)
                        .doctor(doctorPetrov)
                        .patient(patientStoyanov)
                        .build());
        createdSickLeaveId = sickLeave.getId();

        String token = getToken("d.petrov@medical.com", "Az1234!");

        String body = String.format("""
                {
                  "startDate":    "%s",
                  "numberOfDays": 5
                }
                """, LocalDate.now().minusDays(3));

        // @NotTooOld на startDate в UpdateSickLeaveRequest → 400
        mockMvc.perform(put("/api/sick-leaves/" + sickLeave.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ADMIN изтрива болничен лист → 204
    @Test
    void deleteSickLeave_asAdmin_returns204() throws Exception {
        // Създаваме болничен лист директно в базата
        SickLeave sickLeave = sickLeaveRepository.save(
                SickLeave.builder()
                        .examination(freshExam)
                        .startDate(LocalDate.now())
                        .numberOfDays(2)
                        .doctor(doctorPetrov)
                        .patient(patientStoyanov)
                        .build());
        // Не задаваме createdSickLeaveId — очакваме изтриване чрез DELETE endpoint-а

        String token = getToken("admin@medical.com", "Az1234!");

        mockMvc.perform(delete("/api/sick-leaves/" + sickLeave.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
    }
}
