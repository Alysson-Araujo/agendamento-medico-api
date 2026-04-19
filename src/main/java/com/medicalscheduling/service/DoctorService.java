package com.medicalscheduling.service;

import com.medicalscheduling.domain.Doctor;
import com.medicalscheduling.domain.User;
import com.medicalscheduling.domain.Specialty;
import com.medicalscheduling.dto.request.CreateDoctorRequest;
import com.medicalscheduling.dto.request.UpdateDoctorRequest;
import com.medicalscheduling.dto.response.AppointmentResponse;
import com.medicalscheduling.dto.response.DoctorResponse;
import com.medicalscheduling.exception.BusinessException;
import com.medicalscheduling.exception.ResourceNotFoundException;
import com.medicalscheduling.repository.AppointmentRepository;
import com.medicalscheduling.repository.DoctorRepository;
import com.medicalscheduling.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DoctorService(DoctorRepository doctorRepository, UserRepository userRepository,
                         AppointmentRepository appointmentRepository, PasswordEncoder passwordEncoder) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public DoctorResponse create(CreateDoctorRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already registered");
        }
        if (doctorRepository.existsByCrm(request.crm())) {
            throw new BusinessException("CRM already registered");
        }

        var user = new User(
                request.name(),
                request.email(),
                passwordEncoder.encode(request.password()),
                User.Role.DOCTOR
        );
        user = userRepository.save(user);

        var doctor = new Doctor(user, request.crm(), request.specialty(), request.phone());
        doctor = doctorRepository.save(doctor);

        return DoctorResponse.from(doctor);
    }

    public Page<DoctorResponse> listActive(Specialty specialty, Pageable pageable) {
        Page<Doctor> doctors;
        if (specialty != null) {
            doctors = doctorRepository.findByActiveTrueAndSpecialty(specialty, pageable);
        } else {
            doctors = doctorRepository.findByActiveTrue(pageable);
        }
        return doctors.map(DoctorResponse::from);
    }

    public DoctorResponse getById(UUID id) {
        var doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        return DoctorResponse.from(doctor);
    }

    public Page<AppointmentResponse> getDoctorAgenda(UUID doctorId, Pageable pageable) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new ResourceNotFoundException("Doctor not found");
        }
        return appointmentRepository.findByDoctorId(doctorId, pageable)
                .map(AppointmentResponse::from);
    }

    @Transactional
    public DoctorResponse update(UUID id, UpdateDoctorRequest request) {
        var doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        if (request.crm() != null) {
            doctor.setCrm(request.crm());
        }
        if (request.specialty() != null) {
            doctor.setSpecialty(request.specialty());
        }
        if (request.phone() != null) {
            doctor.setPhone(request.phone());
        }
        if (request.name() != null) {
            doctor.getUser().setName(request.name());
            userRepository.save(doctor.getUser());
        }

        doctor = doctorRepository.save(doctor);
        return DoctorResponse.from(doctor);
    }

    @Transactional
    public void delete(UUID id) {
        var doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        doctor.setActive(false);
        doctor.getUser().setActive(false);
        doctorRepository.save(doctor);
        userRepository.save(doctor.getUser());
    }
}
