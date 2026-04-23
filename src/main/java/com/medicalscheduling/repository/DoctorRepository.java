package com.medicalscheduling.repository;

import com.medicalscheduling.domain.Doctor;
import com.medicalscheduling.domain.Specialty;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {
    Page<Doctor> findByActiveTrue(Pageable pageable);
    Page<Doctor> findByActiveTrueAndSpecialty(Specialty specialty, Pageable pageable);
    Optional<Doctor> findByUserId(UUID userId);
    boolean existsByCrm(String crm);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Doctor d WHERE d.id = :id")
    Optional<Doctor> findByIdForUpdate(@Param("id") UUID id);
}
