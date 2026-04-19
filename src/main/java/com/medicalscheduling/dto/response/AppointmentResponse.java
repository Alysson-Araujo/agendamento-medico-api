package com.medicalscheduling.dto.response;

import com.medicalscheduling.domain.Appointment;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID doctorId,
        String doctorName,
        UUID patientId,
        String patientName,
        LocalDateTime dateTime,
        String status,
        String reason,
        String cancelReason,
        LocalDateTime createdAt
) {
    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getDoctor().getId(),
                appointment.getDoctor().getUser().getName(),
                appointment.getPatient().getId(),
                appointment.getPatient().getUser().getName(),
                appointment.getDateTime(),
                appointment.getStatus().name(),
                appointment.getReason(),
                appointment.getCancelReason(),
                appointment.getCreatedAt()
        );
    }
}
