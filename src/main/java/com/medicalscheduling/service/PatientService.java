package com.medicalscheduling.service;

import com.medicalscheduling.domain.Patient;
import com.medicalscheduling.domain.User;
import com.medicalscheduling.dto.request.CreatePatientRequest;
import com.medicalscheduling.dto.request.UpdatePatientRequest;
import com.medicalscheduling.dto.response.PatientResponse;
import com.medicalscheduling.exception.BusinessException;
import com.medicalscheduling.exception.ResourceNotFoundException;
import com.medicalscheduling.repository.PatientRepository;
import com.medicalscheduling.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PatientService(PatientRepository patientRepository, UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public PatientResponse create(CreatePatientRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already registered");
        }
        if (patientRepository.existsByCpf(request.cpf())) {
            throw new BusinessException("CPF already registered");
        }

        var user = new User(
                request.name(),
                request.email(),
                passwordEncoder.encode(request.password()),
                User.Role.PATIENT
        );
        user = userRepository.save(user);

        var patient = new Patient(user, request.cpf(), request.phone(), request.birthDate());
        patient = patientRepository.save(patient);

        return PatientResponse.from(patient);
    }

    public Page<PatientResponse> listActive(Pageable pageable) {
        return patientRepository.findByActiveTrue(pageable)
                .map(PatientResponse::from);
    }

    public PatientResponse getById(UUID id) {
        var patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        return PatientResponse.from(patient);
    }

    public PatientResponse getByUserId(UUID userId) {
        var patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        return PatientResponse.from(patient);
    }

    @Transactional
    public PatientResponse update(UUID id, UpdatePatientRequest request) {
        var patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        if (request.phone() != null) {
            patient.setPhone(request.phone());
        }
        if (request.name() != null) {
            patient.getUser().setName(request.name());
            userRepository.save(patient.getUser());
        }

        patient = patientRepository.save(patient);
        return PatientResponse.from(patient);
    }

    @Transactional
    public void delete(UUID id) {
        var patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        patient.setActive(false);
        patient.getUser().setActive(false);
        patientRepository.save(patient);
        userRepository.save(patient.getUser());
    }
}
