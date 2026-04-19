package com.medicalscheduling.controller;

import com.medicalscheduling.dto.request.CancelAppointmentRequest;
import com.medicalscheduling.dto.request.CreateAppointmentRequest;
import com.medicalscheduling.dto.response.AppointmentResponse;
import com.medicalscheduling.service.AppointmentService;
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
@RequestMapping("/api/appointments")
@Tag(name = "Appointments", description = "Appointment management endpoints")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Schedule a new appointment (PATIENT only)")
    public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody CreateAppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all appointments (ADMIN only)")
    public ResponseEntity<Page<AppointmentResponse>> listAll(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(appointmentService.listAll(pageable));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "List my appointments (PATIENT only)")
    public ResponseEntity<Page<AppointmentResponse>> listMy(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(appointmentService.listMyAppointments(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment details")
    public ResponseEntity<AppointmentResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.getById(id));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN')")
    @Operation(summary = "Cancel appointment (PATIENT owner or ADMIN)")
    public ResponseEntity<AppointmentResponse> cancel(@PathVariable UUID id,
                                                       @Valid @RequestBody CancelAppointmentRequest request) {
        return ResponseEntity.ok(appointmentService.cancel(id, request));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Complete appointment (ADMIN or owning DOCTOR)")
    public ResponseEntity<AppointmentResponse> complete(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.complete(id));
    }
}
