package com.medicalscheduling.controller;

import com.medicalscheduling.domain.Specialty;
import com.medicalscheduling.dto.request.CreateDoctorRequest;
import com.medicalscheduling.dto.request.UpdateDoctorRequest;
import com.medicalscheduling.dto.response.AppointmentResponse;
import com.medicalscheduling.dto.response.DoctorResponse;
import com.medicalscheduling.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/doctors")
@Tag(name = "Doctors", description = "Doctor management endpoints")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new doctor (ADMIN only)")
    public ResponseEntity<DoctorResponse> create(@Valid @RequestBody CreateDoctorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorService.create(request));
    }

    @GetMapping
    @Operation(summary = "List active doctors with optional specialty filter")
    public ResponseEntity<Page<DoctorResponse>> list(
            @RequestParam(required = false) Specialty specialty,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(doctorService.listActive(specialty, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Get doctor details (ADMIN or DOCTOR)")
    public ResponseEntity<DoctorResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(doctorService.getById(id));
    }

    @GetMapping("/{id}/agenda")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Get doctor's appointments (ADMIN or DOCTOR)")
    public ResponseEntity<Page<AppointmentResponse>> getAgenda(
            @PathVariable UUID id,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(doctorService.getDoctorAgenda(id, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update doctor (ADMIN only)")
    public ResponseEntity<DoctorResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody UpdateDoctorRequest request) {
        return ResponseEntity.ok(doctorService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft delete doctor (ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        doctorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
