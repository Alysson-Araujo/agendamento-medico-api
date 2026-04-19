package com.medicalscheduling.dto.request;

import com.medicalscheduling.domain.Specialty;
import jakarta.validation.constraints.Size;

public record UpdateDoctorRequest(
        @Size(max = 20, message = "CRM must be at most 20 characters")
        String crm,

        Specialty specialty,

        @Size(max = 20, message = "Phone must be at most 20 characters")
        String phone,

        @Size(max = 100, message = "Name must be at most 100 characters")
        String name
) {}
