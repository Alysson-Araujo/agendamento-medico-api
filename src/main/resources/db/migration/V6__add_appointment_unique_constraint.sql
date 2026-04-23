-- Prevent double-booking: a doctor cannot have two active appointments at the same date/time.
-- Cancelled appointments are excluded so the slot can be reused after a cancellation.
CREATE UNIQUE INDEX uq_appointments_doctor_datetime_active
    ON appointments(doctor_id, date_time)
    WHERE status <> 'CANCELLED';
