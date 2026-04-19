package com.medicalscheduling.dto.response;

import com.medicalscheduling.domain.Doctor;

import java.util.UUID;

public record DoctorResponse(
        UUID id,
        String name,
        String email,
        String crm,
        String specialty,
        String phone,
        Boolean active
) {
    public static DoctorResponse from(Doctor doctor) {
        return new DoctorResponse(
                doctor.getId(),
                doctor.getUser().getName(),
                doctor.getUser().getEmail(),
                doctor.getCrm(),
                doctor.getSpecialty().name(),
                doctor.getPhone(),
                doctor.getActive()
        );
    }
}
