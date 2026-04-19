package com.medicalscheduling.service;

import com.medicalscheduling.domain.*;
import com.medicalscheduling.dto.request.CancelAppointmentRequest;
import com.medicalscheduling.dto.request.CreateAppointmentRequest;
import com.medicalscheduling.exception.BusinessException;
import com.medicalscheduling.exception.ResourceNotFoundException;
import com.medicalscheduling.repository.AppointmentRepository;
import com.medicalscheduling.repository.DoctorRepository;
import com.medicalscheduling.repository.PatientRepository;
import com.medicalscheduling.repository.UserRepository;
import com.medicalscheduling.validator.AppointmentValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AppointmentValidator appointmentValidator;

    @InjectMocks
    private AppointmentService appointmentService;

    private User patientUser;
    private User doctorUser;
    private Patient patient;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        patientUser = new User("Patient Name", "patient@email.com", "encoded", User.Role.PATIENT);
        patientUser.setId(UUID.randomUUID());

        doctorUser = new User("Doctor Name", "doctor@email.com", "encoded", User.Role.DOCTOR);
        doctorUser.setId(UUID.randomUUID());

        patient = new Patient(patientUser, "123.456.789-00", "11999999999", LocalDate.of(1990, 1, 1));
        patient.setId(UUID.randomUUID());

        doctor = new Doctor(doctorUser, "CRM12345", Specialty.CARDIOLOGY, "11888888888");
        doctor.setId(UUID.randomUUID());

        var auth = new UsernamePasswordAuthenticationToken(
                patientUser.getEmail(), null,
                List.of(new SimpleGrantedAuthority("ROLE_PATIENT"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private LocalDateTime nextValidDateTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime candidate = now.plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        if (candidate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }

    @Test
    void shouldCreateAppointmentSuccessfully() {
        LocalDateTime dateTime = nextValidDateTime();
        var request = new CreateAppointmentRequest(doctor.getId(), dateTime, "Checkup");

        when(userRepository.findByEmail(patientUser.getEmail())).thenReturn(Optional.of(patientUser));
        when(patientRepository.findByUserId(patientUser.getId())).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        doNothing().when(appointmentValidator).validateScheduling(doctor, patient, dateTime);

        var appointment = new Appointment(doctor, patient, dateTime, "Checkup");
        appointment.setId(UUID.randomUUID());
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);

        var response = appointmentService.create(request);

        assertNotNull(response);
        assertEquals("Checkup", response.reason());
        verify(appointmentValidator).validateScheduling(doctor, patient, dateTime);
    }

    @Test
    void shouldThrowWhenDoctorNotFound() {
        LocalDateTime dateTime = nextValidDateTime();
        var request = new CreateAppointmentRequest(UUID.randomUUID(), dateTime, "Checkup");

        when(userRepository.findByEmail(patientUser.getEmail())).thenReturn(Optional.of(patientUser));
        when(patientRepository.findByUserId(patientUser.getId())).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> appointmentService.create(request));
    }

    @Test
    void shouldThrowWhenDoctorInactive() {
        LocalDateTime dateTime = nextValidDateTime();
        var request = new CreateAppointmentRequest(doctor.getId(), dateTime, "Checkup");

        when(userRepository.findByEmail(patientUser.getEmail())).thenReturn(Optional.of(patientUser));
        when(patientRepository.findByUserId(patientUser.getId())).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        doThrow(new BusinessException("Cannot schedule appointment with inactive doctor"))
                .when(appointmentValidator).validateScheduling(doctor, patient, dateTime);

        assertThrows(BusinessException.class, () -> appointmentService.create(request));
    }

    @Test
    void shouldThrowWhenPatientInactive() {
        LocalDateTime dateTime = nextValidDateTime();
        var request = new CreateAppointmentRequest(doctor.getId(), dateTime, "Checkup");

        when(userRepository.findByEmail(patientUser.getEmail())).thenReturn(Optional.of(patientUser));
        when(patientRepository.findByUserId(patientUser.getId())).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        doThrow(new BusinessException("Cannot schedule appointment with inactive patient"))
                .when(appointmentValidator).validateScheduling(doctor, patient, dateTime);

        assertThrows(BusinessException.class, () -> appointmentService.create(request));
    }

    @Test
    void shouldThrowWhenDoctorTimeConflict() {
        LocalDateTime dateTime = nextValidDateTime();
        var request = new CreateAppointmentRequest(doctor.getId(), dateTime, "Checkup");

        when(userRepository.findByEmail(patientUser.getEmail())).thenReturn(Optional.of(patientUser));
        when(patientRepository.findByUserId(patientUser.getId())).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        doThrow(new BusinessException("Doctor already has an appointment at this time"))
                .when(appointmentValidator).validateScheduling(doctor, patient, dateTime);

        assertThrows(BusinessException.class, () -> appointmentService.create(request));
    }

    @Test
    void shouldThrowWhenPatientSameDayAppointment() {
        LocalDateTime dateTime = nextValidDateTime();
        var request = new CreateAppointmentRequest(doctor.getId(), dateTime, "Checkup");

        when(userRepository.findByEmail(patientUser.getEmail())).thenReturn(Optional.of(patientUser));
        when(patientRepository.findByUserId(patientUser.getId())).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        doThrow(new BusinessException("Patient already has an appointment on this day"))
                .when(appointmentValidator).validateScheduling(doctor, patient, dateTime);

        assertThrows(BusinessException.class, () -> appointmentService.create(request));
    }

    @Test
    void shouldThrowWhenOutsideBusinessHours() {
        LocalDateTime dateTime = nextValidDateTime();
        var request = new CreateAppointmentRequest(doctor.getId(), dateTime, "Checkup");

        when(userRepository.findByEmail(patientUser.getEmail())).thenReturn(Optional.of(patientUser));
        when(patientRepository.findByUserId(patientUser.getId())).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        doThrow(new BusinessException("Appointments are only available between 07:00 and 19:00"))
                .when(appointmentValidator).validateScheduling(doctor, patient, dateTime);

        assertThrows(BusinessException.class, () -> appointmentService.create(request));
    }

    @Test
    void shouldThrowWhenLessThan30MinAdvance() {
        LocalDateTime dateTime = nextValidDateTime();
        var request = new CreateAppointmentRequest(doctor.getId(), dateTime, "Checkup");

        when(userRepository.findByEmail(patientUser.getEmail())).thenReturn(Optional.of(patientUser));
        when(patientRepository.findByUserId(patientUser.getId())).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        doThrow(new BusinessException("Appointment must be scheduled at least 30 minutes in advance"))
                .when(appointmentValidator).validateScheduling(doctor, patient, dateTime);

        assertThrows(BusinessException.class, () -> appointmentService.create(request));
    }

    @Test
    void shouldCancelAppointmentSuccessfully() {
        var appointment = new Appointment(doctor, patient, nextValidDateTime().plusDays(5), "Checkup");
        appointment.setId(UUID.randomUUID());
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(userRepository.findByEmail(patientUser.getEmail())).thenReturn(Optional.of(patientUser));
        when(patientRepository.findByUserId(patientUser.getId())).thenReturn(Optional.of(patient));
        doNothing().when(appointmentValidator).validateCancellation(eq(appointment), any(LocalDateTime.class));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);

        var request = new CancelAppointmentRequest("No longer needed");
        var response = appointmentService.cancel(appointment.getId(), request);

        assertNotNull(response);
        verify(appointmentValidator).validateCancellation(eq(appointment), any(LocalDateTime.class));
    }

    @Test
    void shouldThrowWhenCancellingCompletedAppointment() {
        var appointment = new Appointment(doctor, patient, nextValidDateTime(), "Checkup");
        appointment.setId(UUID.randomUUID());
        appointment.setStatus(AppointmentStatus.COMPLETED);

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(userRepository.findByEmail(patientUser.getEmail())).thenReturn(Optional.of(patientUser));
        when(patientRepository.findByUserId(patientUser.getId())).thenReturn(Optional.of(patient));
        doThrow(new BusinessException("Cannot cancel a completed appointment"))
                .when(appointmentValidator).validateCancellation(eq(appointment), any(LocalDateTime.class));

        var request = new CancelAppointmentRequest("Changed mind");

        assertThrows(BusinessException.class, () -> appointmentService.cancel(appointment.getId(), request));
    }

    @Test
    void shouldThrowWhenCancellingWithLessThan24h() {
        var appointment = new Appointment(doctor, patient, LocalDateTime.now().plusHours(12), "Checkup");
        appointment.setId(UUID.randomUUID());
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(userRepository.findByEmail(patientUser.getEmail())).thenReturn(Optional.of(patientUser));
        when(patientRepository.findByUserId(patientUser.getId())).thenReturn(Optional.of(patient));
        doThrow(new BusinessException("Cannot cancel appointment with less than 24 hours in advance"))
                .when(appointmentValidator).validateCancellation(eq(appointment), any(LocalDateTime.class));

        var request = new CancelAppointmentRequest("Emergency");

        assertThrows(BusinessException.class, () -> appointmentService.cancel(appointment.getId(), request));
    }

    @Test
    void shouldCompleteAppointmentSuccessfully() {
        var auth = new UsernamePasswordAuthenticationToken(
                doctorUser.getEmail(), null,
                List.of(new SimpleGrantedAuthority("ROLE_DOCTOR"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        var appointment = new Appointment(doctor, patient, nextValidDateTime(), "Checkup");
        appointment.setId(UUID.randomUUID());
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(userRepository.findByEmail(doctorUser.getEmail())).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.findByUserId(doctorUser.getId())).thenReturn(Optional.of(doctor));
        doNothing().when(appointmentValidator).validateCompletion(appointment);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);

        var response = appointmentService.complete(appointment.getId());

        assertNotNull(response);
        verify(appointmentValidator).validateCompletion(appointment);
    }

    @Test
    void shouldThrowWhenCompletingNonScheduledAppointment() {
        var auth = new UsernamePasswordAuthenticationToken(
                doctorUser.getEmail(), null,
                List.of(new SimpleGrantedAuthority("ROLE_DOCTOR"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        var appointment = new Appointment(doctor, patient, nextValidDateTime(), "Checkup");
        appointment.setId(UUID.randomUUID());
        appointment.setStatus(AppointmentStatus.CANCELLED);

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(userRepository.findByEmail(doctorUser.getEmail())).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.findByUserId(doctorUser.getId())).thenReturn(Optional.of(doctor));
        doThrow(new BusinessException("Only scheduled appointments can be completed"))
                .when(appointmentValidator).validateCompletion(appointment);

        assertThrows(BusinessException.class, () -> appointmentService.complete(appointment.getId()));
    }

    @Test
    void shouldThrowWhenPatientCancelsOthersAppointment() {
        var otherPatientUser = new User("Other", "other@email.com", "encoded", User.Role.PATIENT);
        otherPatientUser.setId(UUID.randomUUID());
        var otherPatient = new Patient(otherPatientUser, "987.654.321-00", "11777777777", LocalDate.of(1985, 5, 5));
        otherPatient.setId(UUID.randomUUID());

        var appointment = new Appointment(doctor, otherPatient, nextValidDateTime().plusDays(5), "Checkup");
        appointment.setId(UUID.randomUUID());
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(userRepository.findByEmail(patientUser.getEmail())).thenReturn(Optional.of(patientUser));
        when(patientRepository.findByUserId(patientUser.getId())).thenReturn(Optional.of(patient));

        var request = new CancelAppointmentRequest("Reason");

        assertThrows(BusinessException.class, () -> appointmentService.cancel(appointment.getId(), request));
    }
}
