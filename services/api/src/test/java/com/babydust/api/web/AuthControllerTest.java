package com.babydust.api.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void wechatLoginReturnsUnifiedResponse() throws Exception {
        mvc.perform(post("/api/v1/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"test-code\",\"nickname\":\"Mom\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists());
    }

    @Test
    void protectedEndpointRejectsMissingSession() throws Exception {
        mvc.perform(post("/api/v1/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No session family\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void devUserHeaderIsAcceptedOnlyWhenExplicitlyEnabled() throws Exception {
        UUID userId = UUID.randomUUID();
        mvc.perform(post("/api/v1/families")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dev family\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ownerUserId").value(userId.toString()));
    }

    @Test
    void babyProfileValidatesRequiredFields() throws Exception {
        String login = mvc.perform(post("/api/v1/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"baby-validation\",\"nickname\":\"Mom\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(login).path("data").path("token").asText();

        mvc.perform(post("/api/v1/profiles/babies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Baby\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
