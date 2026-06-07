package com.medicalrecord.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Базов клас за всички интеграционни тестове — само помощни методи.
 *
 * @SpringBootTest и @AutoConfigureMockMvc са на конкретните тест класове,
 * за да не се опитва IntelliJ да стартира абстрактния клас директно.
 */
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Логва се в системата и връща JWT токен.
     * Извиква реалния /api/auth/login endpoint — преминава през цялата верига:
     * контролер → услуга → база данни → JWT генерация.
     */
    protected String getToken(String email, String password) throws Exception {
        String loginBody = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\"}", email, password);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    // Формира Authorization header стойност
    protected String bearer(String token) {
        return "Bearer " + token;
    }
}
