package com.medicalscheduling.validator;

import com.medicalscheduling.domain.Appointment;
import com.medicalscheduling.domain.AppointmentStatus;
import com.medicalscheduling.domain.Doctor;
import com.medicalscheduling.domain.Patient;
import com.medicalscheduling.exception.BusinessException;
import com.medicalscheduling.repository.AppointmentRepository;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

@Component
public class AppointmentValidator {

    private final AppointmentRepository appointmentRepository;

    public AppointmentValidator(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public void validateScheduling(Doctor doctor, Patient patient, LocalDateTime dateTime) {
        validateScheduling(doctor, patient, dateTime, LocalDateTime.now());
    }

    public void validateScheduling(Doctor doctor, Patient patient, LocalDateTime dateTime, LocalDateTime now) {
        validateDoctorActive(doctor);
        validatePatientActive(patient);
        validateBusinessHours(dateTime);
        validateMinimumAdvance(dateTime, now);
        validateDoctorAvailability(doctor, dateTime);
        validatePatientDailyLimit(patient, dateTime);
    }

    public void validateCancellation(Appointment appointment, LocalDateTime now) {
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessException("Cannot cancel a completed appointment");
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Appointment is already cancelled");
        }
        if (appointment.getDateTime().isBefore(now.plusHours(24))) {
            throw new BusinessException("Cannot cancel appointment with less than 24 hours in advance");
        }
    }

    public void validateCompletion(Appointment appointment) {
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new BusinessException("Only scheduled appointments can be completed");
        }
    }

    private void validateDoctorActive(Doctor doctor) {
        if (!doctor.getActive()) {
            throw new BusinessException("Cannot schedule appointment with inactive doctor");
        }
    }

    private void validatePatientActive(Patient patient) {
        if (!patient.getActive()) {
            throw new BusinessException("Cannot schedule appointment with inactive patient");
        }
    }

    private void validateBusinessHours(LocalDateTime dateTime) {
        DayOfWeek day = dateTime.getDayOfWeek();
        int hour = dateTime.getHour();

        if (day == DayOfWeek.SUNDAY) {
            throw new BusinessException("Appointments are not available on Sundays");
        }
        if (hour < 7 || hour >= 18) {
            throw new BusinessException("Appointments are only available between 07:00 and 18:00");
        }
    }

    private void validateMinimumAdvance(LocalDateTime dateTime, LocalDateTime now) {
        if (dateTime.isBefore(now.plusMinutes(30))) {
            throw new BusinessException("Appointment must be scheduled at least 30 minutes in advance");
        }
    }

    private void validateDoctorAvailability(Doctor doctor, LocalDateTime dateTime) {
        if (appointmentRepository.existsByDoctorIdAndDateTimeAndStatusNot(doctor.getId(), dateTime)) {
            throw new BusinessException("Doctor already has an appointment at this time");
        }
    }

    private void validatePatientDailyLimit(Patient patient, LocalDateTime dateTime) {
        if (appointmentRepository.existsByPatientIdAndDateAndStatusNot(patient.getId(), dateTime)) {
            throw new BusinessException("Patient already has an appointment on this day");
        }
    }
}
