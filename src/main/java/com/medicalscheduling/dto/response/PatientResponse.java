package com.medicalscheduling.dto.response;

import com.medicalscheduling.domain.Patient;

import java.time.LocalDate;
import java.util.UUID;

public record PatientResponse(
        UUID id,
        String name,
        String email,
        String cpf,
        String phone,
        LocalDate birthDate,
        Boolean active
) {
    public static PatientResponse from(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getUser().getName(),
                patient.getUser().getEmail(),
                patient.getCpf(),
                patient.getPhone(),
                patient.getBirthDate(),
                patient.getActive()
        );
    }
}
