package com.medicalscheduling.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medicalscheduling.domain.Specialty;
import com.medicalscheduling.dto.request.CreateDoctorRequest;
import com.medicalscheduling.dto.request.LoginRequest;
import com.medicalscheduling.dto.request.UpdateDoctorRequest;
import com.medicalscheduling.dto.response.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class DoctorControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("medical_scheduling_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = loginAsAdmin();
    }

    private String loginAsAdmin() throws Exception {
        var loginRequest = new LoginRequest("admin@medical.com", "admin123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        TokenResponse tokenResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), TokenResponse.class);
        return tokenResponse.accessToken();
    }

    private String loginAs(String email, String password) throws Exception {
        var loginRequest = new LoginRequest(email, password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        TokenResponse tokenResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), TokenResponse.class);
        return tokenResponse.accessToken();
    }

    private UUID createDoctor(String suffix) throws Exception {
        var request = new CreateDoctorRequest(
                "Dr. " + suffix, "doc_" + suffix + "@test.com", "password123",
                "CRM" + suffix, Specialty.CARDIOLOGY, "11999999999"
        );

        MvcResult result = mockMvc.perform(post("/api/doctors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    @Test
    void shouldCreateDoctor() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var request = new CreateDoctorRequest(
                "Dr. Test " + suffix, "drcreate_" + suffix + "@test.com", "password123",
                "CRMC" + suffix, Specialty.CARDIOLOGY, "11999999999"
        );

        mockMvc.perform(post("/api/doctors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Dr. Test " + suffix))
                .andExpect(jsonPath("$.email").value("drcreate_" + suffix + "@test.com"))
                .andExpect(jsonPath("$.crm").value("CRMC" + suffix))
                .andExpect(jsonPath("$.specialty").value("CARDIOLOGY"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldRejectCreateDoctorWithDuplicateEmail() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var request = new CreateDoctorRequest(
                "Dr. Dup " + suffix, "drdup_" + suffix + "@test.com", "password123",
                "CRMD" + suffix, Specialty.CARDIOLOGY, "11999999999"
        );

        mockMvc.perform(post("/api/doctors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        var request2 = new CreateDoctorRequest(
                "Dr. Dup2 " + suffix, "drdup_" + suffix + "@test.com", "password123",
                "CRME" + suffix, Specialty.DERMATOLOGY, "11888888888"
        );

        mockMvc.perform(post("/api/doctors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldRejectCreateDoctorWithDuplicateCrm() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var request = new CreateDoctorRequest(
                "Dr. CrmDup " + suffix, "drcrm1_" + suffix + "@test.com", "password123",
                "CRMDUP" + suffix, Specialty.CARDIOLOGY, "11999999999"
        );

        mockMvc.perform(post("/api/doctors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        var request2 = new CreateDoctorRequest(
                "Dr. CrmDup2 " + suffix, "drcrm2_" + suffix + "@test.com", "password123",
                "CRMDUP" + suffix, Specialty.DERMATOLOGY, "11888888888"
        );

        mockMvc.perform(post("/api/doctors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldRejectCreateDoctorWithInvalidData() throws Exception {
        var request = new CreateDoctorRequest(
                "", "invalid-email", "123",
                "", null, ""
        );

        mockMvc.perform(post("/api/doctors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectCreateDoctorWithoutAdminRole() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // Create a doctor and login as that doctor
        var doctorRequest = new CreateDoctorRequest(
                "Dr. NoAuth " + suffix, "drnoauth_" + suffix + "@test.com", "password123",
                "CRMN" + suffix, Specialty.CARDIOLOGY, "11999999999"
        );
        mockMvc.perform(post("/api/doctors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(doctorRequest)))
                .andExpect(status().isCreated());

        String doctorToken = loginAs("drnoauth_" + suffix + "@test.com", "password123");

        var newDocRequest = new CreateDoctorRequest(
                "Dr. Unauthorized", "unauthdr@test.com", "password123",
                "CRMX99", Specialty.NEUROLOGY, "11777777777"
        );

        mockMvc.perform(post("/api/doctors")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDocRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldListActiveDoctors() throws Exception {
        mockMvc.perform(get("/api/doctors")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void shouldListActiveDoctorsFilteredBySpecialty() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var request = new CreateDoctorRequest(
                "Dr. Derma " + suffix, "drderma_" + suffix + "@test.com", "password123",
                "CRMDERM" + suffix, Specialty.DERMATOLOGY, "11999999999"
        );
        mockMvc.perform(post("/api/doctors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/doctors")
                        .param("specialty", "DERMATOLOGY")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].specialty").value("DERMATOLOGY"));
    }

    @Test
    void shouldGetDoctorById() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UUID doctorId = createDoctor(suffix);

        mockMvc.perform(get("/api/doctors/" + doctorId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(doctorId.toString()))
                .andExpect(jsonPath("$.name").value("Dr. " + suffix));
    }

    @Test
    void shouldReturn404ForNonExistentDoctor() throws Exception {
        mockMvc.perform(get("/api/doctors/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetDoctorAgenda() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UUID doctorId = createDoctor(suffix);

        mockMvc.perform(get("/api/doctors/" + doctorId + "/agenda")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void shouldUpdateDoctor() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UUID doctorId = createDoctor(suffix);

        var updateRequest = new UpdateDoctorRequest(null, Specialty.NEUROLOGY, "11000000000", "Dr. Updated");

        mockMvc.perform(put("/api/doctors/" + doctorId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dr. Updated"))
                .andExpect(jsonPath("$.specialty").value("NEUROLOGY"))
                .andExpect(jsonPath("$.phone").value("11000000000"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentDoctor() throws Exception {
        var updateRequest = new UpdateDoctorRequest(null, null, "11000000000", null);

        mockMvc.perform(put("/api/doctors/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteDoctor() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UUID doctorId = createDoctor(suffix);

        mockMvc.perform(delete("/api/doctors/" + doctorId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Verify doctor is soft-deleted (still accessible but inactive)
        mockMvc.perform(get("/api/doctors/" + doctorId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentDoctor() throws Exception {
        mockMvc.perform(delete("/api/doctors/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/doctors/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
