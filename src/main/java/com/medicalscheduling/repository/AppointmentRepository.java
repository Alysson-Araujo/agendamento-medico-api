package com.medicalscheduling.repository;

import com.medicalscheduling.domain.Appointment;
import com.medicalscheduling.domain.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Page<Appointment> findByPatientId(UUID patientId, Pageable pageable);

    Page<Appointment> findByDoctorId(UUID doctorId, Pageable pageable);

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.dateTime = :dateTime AND a.status <> 'CANCELLED'")
    boolean existsByDoctorIdAndDateTimeAndStatusNot(
            @Param("doctorId") UUID doctorId,
            @Param("dateTime") LocalDateTime dateTime);

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.patient.id = :patientId " +
           "AND CAST(a.dateTime AS date) = CAST(:dateTime AS date) AND a.status <> 'CANCELLED'")
    boolean existsByPatientIdAndDateAndStatusNot(
            @Param("patientId") UUID patientId,
            @Param("dateTime") LocalDateTime dateTime);
}
