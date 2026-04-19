package com.medicalscheduling.dto.request;

import jakarta.validation.constraints.Size;

public record UpdatePatientRequest(
        @Size(max = 20, message = "Phone must be at most 20 characters")
        String phone,

        @Size(max = 100, message = "Name must be at most 100 characters")
        String name
) {}
