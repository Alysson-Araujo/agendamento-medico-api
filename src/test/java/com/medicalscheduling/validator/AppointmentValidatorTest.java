package com.medicalscheduling.validator;

import com.medicalscheduling.domain.*;
import com.medicalscheduling.exception.BusinessException;
import com.medicalscheduling.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentValidatorTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private AppointmentValidator appointmentValidator;

    private Doctor doctor;
    private Patient patient;

    @BeforeEach
    void setUp() {
        var doctorUser = new User("Dr. Smith", "doctor@email.com", "encoded", User.Role.DOCTOR);
        doctorUser.setId(UUID.randomUUID());
        doctor = new Doctor(doctorUser, "CRM12345", Specialty.CARDIOLOGY, "11999999999");
        doctor.setId(UUID.randomUUID());

        var patientUser = new User("John Doe", "patient@email.com", "encoded", User.Role.PATIENT);
        patientUser.setId(UUID.randomUUID());
        patient = new Patient(patientUser, "123.456.789-00", "11888888888", LocalDate.of(1990, 1, 1));
        patient.setId(UUID.randomUUID());
    }

    private LocalDateTime nextWeekday(int hour) {
        LocalDateTime candidate = LocalDateTime.now().plusDays(3).withHour(hour).withMinute(0).withSecond(0).withNano(0);
        if (candidate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }

    @Test
    void shouldValidateSchedulingSuccessfully() {
        LocalDateTime dateTime = nextWeekday(10);
        LocalDateTime now = dateTime.minusDays(2);

        when(appointmentRepository.existsByDoctorIdAndDateTimeAndStatusNot(doctor.getId(), dateTime))
                .thenReturn(false);
        when(appointmentRepository.existsByPatientIdAndDateAndStatusNot(patient.getId(), dateTime))
                .thenReturn(false);

        assertDoesNotThrow(() -> appointmentValidator.validateScheduling(doctor, patient, dateTime, now));
    }

    @Test
    void shouldThrowWhenDoctorInactive() {
        doctor.setActive(false);
        LocalDateTime dateTime = nextWeekday(10);
        LocalDateTime now = dateTime.minusDays(2);

        assertThrows(BusinessException.class,
                () -> appointmentValidator.validateScheduling(doctor, patient, dateTime, now));
    }

    @Test
    void shouldThrowWhenPatientInactive() {
        patient.setActive(false);
        LocalDateTime dateTime = nextWeekday(10);
        LocalDateTime now = dateTime.minusDays(2);

        assertThrows(BusinessException.class,
                () -> appointmentValidator.validateScheduling(doctor, patient, dateTime, now));
    }

    @Test
    void shouldThrowWhenAppointmentOnSunday() {
        LocalDateTime sunday = LocalDateTime.now()
                .with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
                .withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime now = sunday.minusDays(3);

        assertThrows(BusinessException.class,
                () -> appointmentValidator.validateScheduling(doctor, patient, sunday, now));
    }

    @Test
    void shouldThrowWhenBeforeBusinessHours() {
        LocalDateTime dateTime = nextWeekday(6);
        LocalDateTime now = dateTime.minusDays(2);

        assertThrows(BusinessException.class,
                () -> appointmentValidator.validateScheduling(doctor, patient, dateTime, now));
    }

    @Test
    void shouldThrowWhenAfterBusinessHours() {
        LocalDateTime dateTime = nextWeekday(19);
        LocalDateTime now = dateTime.minusDays(2);

        assertThrows(BusinessException.class,
                () -> appointmentValidator.validateScheduling(doctor, patient, dateTime, now));
    }

    @Test
    void shouldThrowWhenLessThan30MinAdvance() {
        LocalDateTime dateTime = nextWeekday(10);
        LocalDateTime now = dateTime.minusMinutes(15);

        assertThrows(BusinessException.class,
                () -> appointmentValidator.validateScheduling(doctor, patient, dateTime, now));
    }

    @Test
    void shouldThrowWhenDoctorHasConflict() {
        LocalDateTime dateTime = nextWeekday(10);
        LocalDateTime now = dateTime.minusDays(2);

        when(appointmentRepository.existsByDoctorIdAndDateTimeAndStatusNot(doctor.getId(), dateTime))
                .thenReturn(true);

        assertThrows(BusinessException.class,
                () -> appointmentValidator.validateScheduling(doctor, patient, dateTime, now));
    }

    @Test
    void shouldThrowWhenPatientHasAppointmentSameDay() {
        LocalDateTime dateTime = nextWeekday(10);
        LocalDateTime now = dateTime.minusDays(2);

        when(appointmentRepository.existsByDoctorIdAndDateTimeAndStatusNot(doctor.getId(), dateTime))
                .thenReturn(false);
        when(appointmentRepository.existsByPatientIdAndDateAndStatusNot(patient.getId(), dateTime))
                .thenReturn(true);

        assertThrows(BusinessException.class,
                () -> appointmentValidator.validateScheduling(doctor, patient, dateTime, now));
    }

    @Test
    void shouldValidateCancellationSuccessfully() {
        var appointment = new Appointment(doctor, patient, nextWeekday(10).plusDays(5), "Checkup");
        appointment.setId(UUID.randomUUID());
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        LocalDateTime now = appointment.getDateTime().minusDays(3);

        assertDoesNotThrow(() -> appointmentValidator.validateCancellation(appointment, now));
    }

    @Test
    void shouldThrowWhenCancellingCompletedAppointment() {
        var appointment = new Appointment(doctor, patient, nextWeekday(10), "Checkup");
        appointment.setId(UUID.randomUUID());
        appointment.setStatus(AppointmentStatus.COMPLETED);

        var ex = assertThrows(BusinessException.class,
                () -> appointmentValidator.validateCancellation(appointment, LocalDateTime.now()));
        assertEquals("Cannot cancel a completed appointment", ex.getMessage());
    }

    @Test
    void shouldThrowWhenCancellingAlreadyCancelledAppointment() {
        var appointment = new Appointment(doctor, patient, nextWeekday(10), "Checkup");
        appointment.setId(UUID.randomUUID());
        appointment.setStatus(AppointmentStatus.CANCELLED);

        var ex = assertThrows(BusinessException.class,
                () -> appointmentValidator.validateCancellation(appointment, LocalDateTime.now()));
        assertEquals("Appointment is already cancelled", ex.getMessage());
    }

    @Test
    void shouldThrowWhenCancellingWithLessThan24hAdvance() {
        var appointment = new Appointment(doctor, patient, LocalDateTime.now().plusHours(12), "Checkup");
        appointment.setId(UUID.randomUUID());
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        var ex = assertThrows(BusinessException.class,
                () -> appointmentValidator.validateCancellation(appointment, LocalDateTime.now()));
        assertEquals("Cannot cancel appointment with less than 24 hours in advance", ex.getMessage());
    }

    @Test
    void shouldValidateCompletionSuccessfully() {
        var appointment = new Appointment(doctor, patient, nextWeekday(10), "Checkup");
        appointment.setId(UUID.randomUUID());
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        assertDoesNotThrow(() -> appointmentValidator.validateCompletion(appointment));
    }

    @Test
    void shouldThrowWhenCompletingCancelledAppointment() {
        var appointment = new Appointment(doctor, patient, nextWeekday(10), "Checkup");
        appointment.setId(UUID.randomUUID());
        appointment.setStatus(AppointmentStatus.CANCELLED);

        var ex = assertThrows(BusinessException.class,
                () -> appointmentValidator.validateCompletion(appointment));
        assertEquals("Only scheduled appointments can be completed", ex.getMessage());
    }

    @Test
    void shouldThrowWhenCompletingAlreadyCompletedAppointment() {
        var appointment = new Appointment(doctor, patient, nextWeekday(10), "Checkup");
        appointment.setId(UUID.randomUUID());
        appointment.setStatus(AppointmentStatus.COMPLETED);

        var ex = assertThrows(BusinessException.class,
                () -> appointmentValidator.validateCompletion(appointment));
        assertEquals("Only scheduled appointments can be completed", ex.getMessage());
    }

    @Test
    void shouldAcceptAppointmentAtStartOfBusinessHours() {
        LocalDateTime dateTime = nextWeekday(7);
        LocalDateTime now = dateTime.minusDays(2);

        when(appointmentRepository.existsByDoctorIdAndDateTimeAndStatusNot(doctor.getId(), dateTime))
                .thenReturn(false);
        when(appointmentRepository.existsByPatientIdAndDateAndStatusNot(patient.getId(), dateTime))
                .thenReturn(false);

        assertDoesNotThrow(() -> appointmentValidator.validateScheduling(doctor, patient, dateTime, now));
    }

    @Test
    void shouldThrowWhenAtClosingTime() {
        LocalDateTime dateTime = nextWeekday(18);
        LocalDateTime now = dateTime.minusDays(2);

        assertThrows(BusinessException.class,
                () -> appointmentValidator.validateScheduling(doctor, patient, dateTime, now));
    }

    @Test
    void shouldAcceptAppointmentAtEndOfBusinessHours() {
        LocalDateTime dateTime = nextWeekday(17);
        LocalDateTime now = dateTime.minusDays(2);

        when(appointmentRepository.existsByDoctorIdAndDateTimeAndStatusNot(doctor.getId(), dateTime))
                .thenReturn(false);
        when(appointmentRepository.existsByPatientIdAndDateAndStatusNot(patient.getId(), dateTime))
                .thenReturn(false);

        assertDoesNotThrow(() -> appointmentValidator.validateScheduling(doctor, patient, dateTime, now));
    }
}
