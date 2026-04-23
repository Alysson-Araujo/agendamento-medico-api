package com.medicalscheduling.service;

import com.medicalscheduling.domain.Patient;
import com.medicalscheduling.domain.User;
import com.medicalscheduling.domain.Appointment;
import com.medicalscheduling.domain.AppointmentStatus;
import com.medicalscheduling.domain.Doctor;
import com.medicalscheduling.domain.Specialty;
import com.medicalscheduling.dto.request.CreatePatientRequest;
import com.medicalscheduling.dto.request.UpdatePatientRequest;
import com.medicalscheduling.exception.BusinessException;
import com.medicalscheduling.exception.ResourceNotFoundException;
import com.medicalscheduling.repository.AppointmentRepository;
import com.medicalscheduling.repository.PatientRepository;
import com.medicalscheduling.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PatientService patientService;

    private User patientUser;
    private Patient patient;

    @BeforeEach
    void setUp() {
        patientUser = new User("John Doe", "patient@email.com", "encodedPassword", User.Role.PATIENT);
        patientUser.setId(UUID.randomUUID());

        patient = new Patient(patientUser, "123.456.789-00", "11999999999", LocalDate.of(1990, 1, 1));
        patient.setId(UUID.randomUUID());
    }

    @Test
    void shouldCreatePatientSuccessfully() {
        var request = new CreatePatientRequest(
                "John Doe", "patient@email.com", "password123",
                "123.456.789-00", "11999999999", LocalDate.of(1990, 1, 1)
        );

        when(userRepository.existsByEmail("patient@email.com")).thenReturn(false);
        when(patientRepository.existsByCpf("123.456.789-00")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(patientUser);
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);

        var response = patientService.create(request);

        assertNotNull(response);
        assertEquals("John Doe", response.name());
        assertEquals("123.456.789-00", response.cpf());
        assertEquals("11999999999", response.phone());
        verify(userRepository).save(any(User.class));
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    void shouldThrowWhenEmailAlreadyRegistered() {
        var request = new CreatePatientRequest(
                "John Doe", "existing@email.com", "password123",
                "123.456.789-00", "11999999999", LocalDate.of(1990, 1, 1)
        );

        when(userRepository.existsByEmail("existing@email.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> patientService.create(request));
        verify(patientRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenCpfAlreadyRegistered() {
        var request = new CreatePatientRequest(
                "John Doe", "patient@email.com", "password123",
                "123.456.789-00", "11999999999", LocalDate.of(1990, 1, 1)
        );

        when(userRepository.existsByEmail("patient@email.com")).thenReturn(false);
        when(patientRepository.existsByCpf("123.456.789-00")).thenReturn(true);

        assertThrows(BusinessException.class, () -> patientService.create(request));
        verify(patientRepository, never()).save(any());
    }

    @Test
    void shouldListActivePatients() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Patient> patientPage = new PageImpl<>(List.of(patient));

        when(patientRepository.findByActiveTrue(pageable)).thenReturn(patientPage);

        var response = patientService.listActive(pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals("John Doe", response.getContent().get(0).name());
    }

    @Test
    void shouldGetPatientById() {
        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));

        var response = patientService.getById(patient.getId());

        assertNotNull(response);
        assertEquals(patient.getId(), response.id());
        assertEquals("John Doe", response.name());
    }

    @Test
    void shouldThrowWhenPatientNotFoundById() {
        UUID id = UUID.randomUUID();
        when(patientRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> patientService.getById(id));
    }

    @Test
    void shouldGetPatientByUserId() {
        when(patientRepository.findByUserId(patientUser.getId())).thenReturn(Optional.of(patient));

        var response = patientService.getByUserId(patientUser.getId());

        assertNotNull(response);
        assertEquals("John Doe", response.name());
    }

    @Test
    void shouldThrowWhenPatientNotFoundByUserId() {
        UUID userId = UUID.randomUUID();
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> patientService.getByUserId(userId));
    }

    @Test
    void shouldUpdatePatientAllFields() {
        var request = new UpdatePatientRequest("11888888888", "Jane Doe");

        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);
        when(userRepository.save(any(User.class))).thenReturn(patientUser);

        var response = patientService.update(patient.getId(), request);

        assertNotNull(response);
        verify(patientRepository).save(patient);
        verify(userRepository).save(patientUser);
    }

    @Test
    void shouldUpdatePatientPartialFields() {
        var request = new UpdatePatientRequest("11888888888", null);

        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);

        var response = patientService.update(patient.getId(), request);

        assertNotNull(response);
        verify(patientRepository).save(patient);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentPatient() {
        UUID id = UUID.randomUUID();
        var request = new UpdatePatientRequest("11888888888", null);

        when(patientRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> patientService.update(id, request));
    }

    @Test
    void shouldDeletePatientSuccessfully() {
        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(appointmentRepository.findByPatientIdAndStatusAndDateTimeAfter(
                eq(patient.getId()), eq(AppointmentStatus.SCHEDULED), any(LocalDateTime.class)))
                .thenReturn(List.of());

        patientService.delete(patient.getId());

        assertFalse(patient.getActive());
        assertFalse(patientUser.getActive());
        verify(patientRepository).save(patient);
        verify(userRepository).save(patientUser);
        verify(appointmentRepository, never()).saveAll(any());
    }

    @Test
    void shouldCancelFutureAppointmentsWhenDeletingPatient() {
        var doctorUser = new User("Doctor", "doctor@email.com", "encoded", User.Role.DOCTOR);
        doctorUser.setId(UUID.randomUUID());
        var doctor = new Doctor(doctorUser, "CRM12345", Specialty.CARDIOLOGY, "11888888888");
        doctor.setId(UUID.randomUUID());

        var appointment = new Appointment(doctor, patient, LocalDateTime.now().plusDays(3), "Checkup");
        appointment.setId(UUID.randomUUID());
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(appointmentRepository.findByPatientIdAndStatusAndDateTimeAfter(
                eq(patient.getId()), eq(AppointmentStatus.SCHEDULED), any(LocalDateTime.class)))
                .thenReturn(List.of(appointment));
        when(appointmentRepository.saveAll(anyList())).thenReturn(List.of(appointment));

        patientService.delete(patient.getId());

        assertEquals(AppointmentStatus.CANCELLED, appointment.getStatus());
        assertEquals("Patient deactivated", appointment.getCancelReason());
        verify(appointmentRepository).saveAll(anyList());
    }

    @Test
    void shouldThrowWhenDeletingNonExistentPatient() {
        UUID id = UUID.randomUUID();
        when(patientRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> patientService.delete(id));
    }
}
