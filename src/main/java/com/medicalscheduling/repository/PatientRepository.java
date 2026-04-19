package com.medicalscheduling.repository;

import com.medicalscheduling.domain.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    Page<Patient> findByActiveTrue(Pageable pageable);
    Optional<Patient> findByUserId(UUID userId);
    boolean existsByCpf(String cpf);
}
