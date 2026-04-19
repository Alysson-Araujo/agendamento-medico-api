package com.medicalscheduling.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CancelAppointmentRequest(
        @NotBlank(message = "Cancel reason is required")
        String cancelReason
) {}
