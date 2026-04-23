package com.medicalscheduling.service;

import com.medicalscheduling.domain.Doctor;
import com.medicalscheduling.domain.Specialty;
import com.medicalscheduling.domain.User;
import com.medicalscheduling.domain.Appointment;
import com.medicalscheduling.domain.AppointmentStatus;
import com.medicalscheduling.domain.Patient;
import com.medicalscheduling.dto.request.CreateDoctorRequest;
import com.medicalscheduling.dto.request.UpdateDoctorRequest;
import com.medicalscheduling.exception.BusinessException;
import com.medicalscheduling.exception.ResourceNotFoundException;
import com.medicalscheduling.repository.AppointmentRepository;
import com.medicalscheduling.repository.DoctorRepository;
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
class DoctorServiceTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DoctorService doctorService;

    private User doctorUser;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        doctorUser = new User("Dr. Smith", "doctor@email.com", "encodedPassword", User.Role.DOCTOR);
        doctorUser.setId(UUID.randomUUID());

        doctor = new Doctor(doctorUser, "CRM12345", Specialty.CARDIOLOGY, "11999999999");
        doctor.setId(UUID.randomUUID());
    }

    @Test
    void shouldCreateDoctorSuccessfully() {
        var request = new CreateDoctorRequest(
                "Dr. Smith", "doctor@email.com", "password123",
                "CRM12345", Specialty.CARDIOLOGY, "11999999999"
        );

        when(userRepository.existsByEmail("doctor@email.com")).thenReturn(false);
        when(doctorRepository.existsByCrm("CRM12345")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(doctorUser);
        when(doctorRepository.save(any(Doctor.class))).thenReturn(doctor);

        var response = doctorService.create(request);

        assertNotNull(response);
        assertEquals("Dr. Smith", response.name());
        assertEquals("CRM12345", response.crm());
        assertEquals("CARDIOLOGY", response.specialty());
        verify(userRepository).save(any(User.class));
        verify(doctorRepository).save(any(Doctor.class));
    }

    @Test
    void shouldThrowWhenEmailAlreadyRegistered() {
        var request = new CreateDoctorRequest(
                "Dr. Smith", "existing@email.com", "password123",
                "CRM12345", Specialty.CARDIOLOGY, "11999999999"
        );

        when(userRepository.existsByEmail("existing@email.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> doctorService.create(request));
        verify(doctorRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenCrmAlreadyRegistered() {
        var request = new CreateDoctorRequest(
                "Dr. Smith", "doctor@email.com", "password123",
                "CRM12345", Specialty.CARDIOLOGY, "11999999999"
        );

        when(userRepository.existsByEmail("doctor@email.com")).thenReturn(false);
        when(doctorRepository.existsByCrm("CRM12345")).thenReturn(true);

        assertThrows(BusinessException.class, () -> doctorService.create(request));
        verify(doctorRepository, never()).save(any());
    }

    @Test
    void shouldListActiveDoctorsWithoutFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Doctor> doctorPage = new PageImpl<>(List.of(doctor));

        when(doctorRepository.findByActiveTrue(pageable)).thenReturn(doctorPage);

        var response = doctorService.listActive(null, pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        verify(doctorRepository).findByActiveTrue(pageable);
        verify(doctorRepository, never()).findByActiveTrueAndSpecialty(any(), any());
    }

    @Test
    void shouldListActiveDoctorsWithSpecialtyFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Doctor> doctorPage = new PageImpl<>(List.of(doctor));

        when(doctorRepository.findByActiveTrueAndSpecialty(Specialty.CARDIOLOGY, pageable))
                .thenReturn(doctorPage);

        var response = doctorService.listActive(Specialty.CARDIOLOGY, pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        verify(doctorRepository).findByActiveTrueAndSpecialty(Specialty.CARDIOLOGY, pageable);
        verify(doctorRepository, never()).findByActiveTrue(any());
    }

    @Test
    void shouldGetDoctorById() {
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));

        var response = doctorService.getById(doctor.getId());

        assertNotNull(response);
        assertEquals(doctor.getId(), response.id());
        assertEquals("Dr. Smith", response.name());
    }

    @Test
    void shouldThrowWhenDoctorNotFoundById() {
        UUID id = UUID.randomUUID();
        when(doctorRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> doctorService.getById(id));
    }

    @Test
    void shouldGetDoctorAgenda() {
        Pageable pageable = PageRequest.of(0, 10);
        when(doctorRepository.existsById(doctor.getId())).thenReturn(true);
        when(appointmentRepository.findByDoctorId(doctor.getId(), pageable))
                .thenReturn(Page.empty());

        var response = doctorService.getDoctorAgenda(doctor.getId(), pageable);

        assertNotNull(response);
        verify(appointmentRepository).findByDoctorId(doctor.getId(), pageable);
    }

    @Test
    void shouldThrowWhenDoctorNotFoundForAgenda() {
        UUID id = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        when(doctorRepository.existsById(id)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> doctorService.getDoctorAgenda(id, pageable));
    }

    @Test
    void shouldUpdateDoctorAllFields() {
        var request = new UpdateDoctorRequest("CRM99999", Specialty.NEUROLOGY, "11888888888", "Dr. Updated");

        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        when(doctorRepository.save(any(Doctor.class))).thenReturn(doctor);
        when(userRepository.save(any(User.class))).thenReturn(doctorUser);

        var response = doctorService.update(doctor.getId(), request);

        assertNotNull(response);
        verify(doctorRepository).save(doctor);
        verify(userRepository).save(doctorUser);
    }

    @Test
    void shouldUpdateDoctorPartialFields() {
        var request = new UpdateDoctorRequest(null, null, "11888888888", null);

        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        when(doctorRepository.save(any(Doctor.class))).thenReturn(doctor);

        var response = doctorService.update(doctor.getId(), request);

        assertNotNull(response);
        verify(doctorRepository).save(doctor);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentDoctor() {
        UUID id = UUID.randomUUID();
        var request = new UpdateDoctorRequest("CRM99999", null, null, null);

        when(doctorRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> doctorService.update(id, request));
    }

    @Test
    void shouldDeleteDoctorSuccessfully() {
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findByDoctorIdAndStatusAndDateTimeAfter(
                eq(doctor.getId()), eq(AppointmentStatus.SCHEDULED), any(LocalDateTime.class)))
                .thenReturn(List.of());

        doctorService.delete(doctor.getId());

        assertFalse(doctor.getActive());
        assertFalse(doctorUser.getActive());
        verify(doctorRepository).save(doctor);
        verify(userRepository).save(doctorUser);
        verify(appointmentRepository, never()).saveAll(any());
    }

    @Test
    void shouldCancelFutureAppointmentsWhenDeletingDoctor() {
        var patientUser = new User("Patient", "patient@email.com", "encoded", User.Role.PATIENT);
        patientUser.setId(UUID.randomUUID());
        var patient = new Patient(patientUser, "123.456.789-00", "11999999999", LocalDate.of(1990, 1, 1));
        patient.setId(UUID.randomUUID());

        var appointment = new Appointment(doctor, patient, LocalDateTime.now().plusDays(3), "Checkup");
        appointment.setId(UUID.randomUUID());
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findByDoctorIdAndStatusAndDateTimeAfter(
                eq(doctor.getId()), eq(AppointmentStatus.SCHEDULED), any(LocalDateTime.class)))
                .thenReturn(List.of(appointment));
        when(appointmentRepository.saveAll(anyList())).thenReturn(List.of(appointment));

        doctorService.delete(doctor.getId());

        assertEquals(AppointmentStatus.CANCELLED, appointment.getStatus());
        assertEquals("Doctor deactivated", appointment.getCancelReason());
        verify(appointmentRepository).saveAll(anyList());
    }

    @Test
    void shouldThrowWhenDeletingNonExistentDoctor() {
        UUID id = UUID.randomUUID();
        when(doctorRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> doctorService.delete(id));
    }
}
