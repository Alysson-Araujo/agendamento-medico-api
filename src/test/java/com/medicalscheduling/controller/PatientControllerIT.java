package com.medicalscheduling.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medicalscheduling.dto.request.CreatePatientRequest;
import com.medicalscheduling.dto.request.LoginRequest;
import com.medicalscheduling.dto.request.UpdatePatientRequest;
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

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class PatientControllerIT {

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

    private UUID createPatient(String suffix) throws Exception {
        // Use full 8-char suffix across the CPF to avoid collision between concurrent tests
        // Format: "123.XXX.XXX-YY" where X/Y come from suffix chars (16^8 unique combinations)
        String cpf = "123." + suffix.substring(0, 3) + "." + suffix.substring(3, 6) + "-" + suffix.substring(6, 8);
        var request = new CreatePatientRequest(
                "Patient " + suffix, "pat_" + suffix + "@test.com", "password123",
                cpf, "11999999999",
                LocalDate.of(1990, 1, 1)
        );

        MvcResult result = mockMvc.perform(post("/api/patients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    @Test
    void shouldCreatePatient() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var request = new CreatePatientRequest(
                "Patient Test " + suffix, "patcreate_" + suffix + "@test.com", "password123",
                suffix.substring(0, 3) + ".456.789-00", "11999999999",
                LocalDate.of(1990, 5, 15)
        );

        mockMvc.perform(post("/api/patients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Patient Test " + suffix))
                .andExpect(jsonPath("$.email").value("patcreate_" + suffix + "@test.com"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldRejectCreatePatientWithDuplicateEmail() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var request = new CreatePatientRequest(
                "Patient Dup " + suffix, "patdup_" + suffix + "@test.com", "password123",
                suffix.substring(0, 3) + ".111.111-11", "11999999999",
                LocalDate.of(1990, 1, 1)
        );

        mockMvc.perform(post("/api/patients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        var request2 = new CreatePatientRequest(
                "Patient Dup2 " + suffix, "patdup_" + suffix + "@test.com", "password123",
                suffix.substring(0, 3) + ".222.222-22", "11888888888",
                LocalDate.of(1985, 6, 20)
        );

        mockMvc.perform(post("/api/patients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldRejectCreatePatientWithDuplicateCpf() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String cpf = suffix.substring(0, 3) + ".333.333-33";

        var request = new CreatePatientRequest(
                "Patient CpfDup " + suffix, "patcpf1_" + suffix + "@test.com", "password123",
                cpf, "11999999999", LocalDate.of(1990, 1, 1)
        );

        mockMvc.perform(post("/api/patients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        var request2 = new CreatePatientRequest(
                "Patient CpfDup2 " + suffix, "patcpf2_" + suffix + "@test.com", "password123",
                cpf, "11888888888", LocalDate.of(1985, 3, 10)
        );

        mockMvc.perform(post("/api/patients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldRejectCreatePatientWithInvalidData() throws Exception {
        var request = new CreatePatientRequest(
                "", "invalid-email", "123",
                "", "", null
        );

        mockMvc.perform(post("/api/patients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectCreatePatientWithoutAdminRole() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // Create a patient and login as patient
        createPatient(suffix);
        String patientToken = loginAs("pat_" + suffix + "@test.com", "password123");

        var newPatRequest = new CreatePatientRequest(
                "Unauthorized Patient", "unauth@test.com", "password123",
                "999.999.999-99", "11777777777", LocalDate.of(1990, 1, 1)
        );

        mockMvc.perform(post("/api/patients")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPatRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldListActivePatients() throws Exception {
        mockMvc.perform(get("/api/patients")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void shouldGetPatientById() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UUID patientId = createPatient(suffix);

        mockMvc.perform(get("/api/patients/" + patientId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(patientId.toString()))
                .andExpect(jsonPath("$.name").value("Patient " + suffix));
    }

    @Test
    void shouldReturn404ForNonExistentPatient() throws Exception {
        mockMvc.perform(get("/api/patients/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdatePatient() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UUID patientId = createPatient(suffix);

        var updateRequest = new UpdatePatientRequest("11000000000", "Updated Name");

        mockMvc.perform(put("/api/patients/" + patientId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.phone").value("11000000000"));
    }

    @Test
    void shouldUpdatePatientPartially() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UUID patientId = createPatient(suffix);

        var updateRequest = new UpdatePatientRequest("11555555555", null);

        mockMvc.perform(put("/api/patients/" + patientId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("11555555555"))
                .andExpect(jsonPath("$.name").value("Patient " + suffix));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentPatient() throws Exception {
        var updateRequest = new UpdatePatientRequest("11000000000", null);

        mockMvc.perform(put("/api/patients/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeletePatient() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UUID patientId = createPatient(suffix);

        mockMvc.perform(delete("/api/patients/" + patientId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Verify patient is soft-deleted (still accessible but inactive)
        mockMvc.perform(get("/api/patients/" + patientId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentPatient() throws Exception {
        mockMvc.perform(delete("/api/patients/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowPatientToViewOwnProfile() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UUID patientId = createPatient(suffix);

        String patientToken = loginAs("pat_" + suffix + "@test.com", "password123");

        mockMvc.perform(get("/api/patients/" + patientId)
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(patientId.toString()));
    }

    @Test
    void shouldAllowPatientToUpdateOwnProfile() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UUID patientId = createPatient(suffix);

        String patientToken = loginAs("pat_" + suffix + "@test.com", "password123");

        var updateRequest = new UpdatePatientRequest("11222222222", "Self Updated");

        mockMvc.perform(put("/api/patients/" + patientId)
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Self Updated"))
                .andExpect(jsonPath("$.phone").value("11222222222"));
    }
}
