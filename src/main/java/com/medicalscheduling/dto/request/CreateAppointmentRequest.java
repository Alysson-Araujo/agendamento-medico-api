package com.medicalscheduling.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateAppointmentRequest(
        @NotNull(message = "Doctor ID is required")
        UUID doctorId,

        @NotNull(message = "Appointment date/time is required")
        @Future(message = "Appointment date/time must be in the future")
        LocalDateTime dateTime,

        @NotBlank(message = "Reason is required")
        String reason
) {}
