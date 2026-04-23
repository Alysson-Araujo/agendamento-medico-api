package com.medicalscheduling.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medicalscheduling.dto.request.*;
import com.medicalscheduling.dto.response.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AppointmentControllerIT {

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
    private String patientToken;
    private UUID doctorId;
    private UUID patientId;

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

    private String registerAndLoginPatient(String suffix) throws Exception {
        var registerRequest = new RegisterRequest("Patient " + suffix, "patient_" + suffix + "@test.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        var loginRequest = new LoginRequest("patient_" + suffix + "@test.com", "password123");
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
                "Dr. " + suffix,
                "doctor_" + suffix + "@test.com",
                "password123",
                "CRM" + suffix,
                com.medicalscheduling.domain.Specialty.CARDIOLOGY,
                "11999999999"
        );

        MvcResult result = mockMvc.perform(post("/api/doctors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(responseBody).get("id").asText());
    }

    private UUID createPatientViaAdmin(String suffix) throws Exception {
        // CPF must be at most 14 chars (format XXX.XXX.XXX-XX); derive 2-digit suffix from suffix hash
        String cpfSuffix = String.format("%02d", Math.abs(suffix.hashCode()) % 100);
        var request = new CreatePatientRequest(
                "Patient " + suffix,
                "patient_admin_" + suffix + "@test.com",
                "password123",
                "123.456.789-" + cpfSuffix,
                "11999999999",
                LocalDate.of(1990, 1, 1)
        );

        MvcResult result = mockMvc.perform(post("/api/patients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(responseBody).get("id").asText());
    }

    private LocalDateTime nextValidDateTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime candidate = now.plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
        if (candidate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }

    @Test
    void shouldCompleteFullAppointmentFlow() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        doctorId = createDoctor(uniqueSuffix);

        String patientSuffix = UUID.randomUUID().toString().substring(0, 8);
        patientToken = registerAndLoginPatient(patientSuffix);

        // Create patient profile via admin for the registered patient user
        // The patient registered via /api/auth/register doesn't have a Patient record yet
        // We need to create an appointment - but we need a Patient record
        // Let's create doctor and patient via admin, then use admin to test flow

        UUID adminPatientId = createPatientViaAdmin(uniqueSuffix);

        // Login as the admin-created patient
        var loginRequest = new LoginRequest("patient_admin_" + uniqueSuffix + "@test.com", "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        TokenResponse tokenResp = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), TokenResponse.class);
        String patientTokenForTest = tokenResp.accessToken();

        LocalDateTime dateTime = nextValidDateTime();
        var appointmentRequest = new CreateAppointmentRequest(doctorId, dateTime, "Annual checkup");

        // Schedule appointment
        MvcResult appointmentResult = mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + patientTokenForTest)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appointmentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andReturn();

        String appointmentId = objectMapper.readTree(
                appointmentResult.getResponse().getContentAsString()).get("id").asText();

        // Cancel appointment
        var cancelRequest = new CancelAppointmentRequest("Cannot attend");
        mockMvc.perform(patch("/api/appointments/" + appointmentId + "/cancel")
                        .header("Authorization", "Bearer " + patientTokenForTest)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelReason").value("Cannot attend"));
    }

    @Test
    void shouldRejectDuplicateAppointmentOnSameDay() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        UUID docId1 = createDoctor("dup1_" + uniqueSuffix);
        UUID docId2 = createDoctor("dup2_" + uniqueSuffix);

        UUID adminPatientId = createPatientViaAdmin("dup_" + uniqueSuffix);

        var loginRequest = new LoginRequest("patient_admin_dup_" + uniqueSuffix + "@test.com", "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        TokenResponse tokenResp = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), TokenResponse.class);
        String patToken = tokenResp.accessToken();

        LocalDateTime dateTime = nextValidDateTime();

        // First appointment - should succeed
        var request1 = new CreateAppointmentRequest(docId1, dateTime, "First visit");
        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + patToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Second appointment same day - should fail
        var request2 = new CreateAppointmentRequest(docId2, dateTime.plusHours(2), "Second visit");
        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + patToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isUnprocessableEntity());
    }
}
