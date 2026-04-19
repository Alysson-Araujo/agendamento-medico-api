package com.medicalscheduling.service;

import com.medicalscheduling.domain.Appointment;
import com.medicalscheduling.domain.AppointmentStatus;
import com.medicalscheduling.dto.request.CancelAppointmentRequest;
import com.medicalscheduling.dto.request.CreateAppointmentRequest;
import com.medicalscheduling.dto.response.AppointmentResponse;
import com.medicalscheduling.exception.BusinessException;
import com.medicalscheduling.exception.ResourceNotFoundException;
import com.medicalscheduling.repository.AppointmentRepository;
import com.medicalscheduling.repository.DoctorRepository;
import com.medicalscheduling.repository.PatientRepository;
import com.medicalscheduling.repository.UserRepository;
import com.medicalscheduling.validator.AppointmentValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final AppointmentValidator appointmentValidator;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorRepository doctorRepository,
                              PatientRepository patientRepository,
                              UserRepository userRepository,
                              AppointmentValidator appointmentValidator) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.appointmentValidator = appointmentValidator;
    }

    @Transactional
    public AppointmentResponse create(CreateAppointmentRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var patient = patientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
        var doctor = doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        appointmentValidator.validateScheduling(doctor, patient, request.dateTime());

        var appointment = new Appointment(doctor, patient, request.dateTime(), request.reason());
        appointment = appointmentRepository.save(appointment);

        return AppointmentResponse.from(appointment);
    }

    public Page<AppointmentResponse> listAll(Pageable pageable) {
        return appointmentRepository.findAll(pageable)
                .map(AppointmentResponse::from);
    }

    public Page<AppointmentResponse> listMyAppointments(Pageable pageable) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var patient = patientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));

        return appointmentRepository.findByPatientId(patient.getId(), pageable)
                .map(AppointmentResponse::from);
    }

    public AppointmentResponse getById(UUID id) {
        var appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() == com.medicalscheduling.domain.User.Role.PATIENT) {
            var patient = patientRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
            if (!appointment.getPatient().getId().equals(patient.getId())) {
                throw new BusinessException("Access denied: you can only view your own appointments");
            }
        } else if (user.getRole() == com.medicalscheduling.domain.User.Role.DOCTOR) {
            var doctor = doctorRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
            if (!appointment.getDoctor().getId().equals(doctor.getId())) {
                throw new BusinessException("Access denied: you can only view your own appointments");
            }
        }

        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse cancel(UUID id, CancelAppointmentRequest request) {
        var appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() == com.medicalscheduling.domain.User.Role.PATIENT) {
            var patient = patientRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
            if (!appointment.getPatient().getId().equals(patient.getId())) {
                throw new BusinessException("Patient can only cancel their own appointments");
            }
        }

        appointmentValidator.validateCancellation(appointment, LocalDateTime.now());

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelReason(request.cancelReason());
        appointment = appointmentRepository.save(appointment);

        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse complete(UUID id) {
        var appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() == com.medicalscheduling.domain.User.Role.DOCTOR) {
            var doctor = doctorRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
            if (!appointment.getDoctor().getId().equals(doctor.getId())) {
                throw new BusinessException("Doctor can only complete their own appointments");
            }
        } else if (user.getRole() != com.medicalscheduling.domain.User.Role.ADMIN) {
            throw new BusinessException("Only ADMIN or the owning DOCTOR can complete appointments");
        }

        appointmentValidator.validateCompletion(appointment);

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment = appointmentRepository.save(appointment);

        return AppointmentResponse.from(appointment);
    }
}
