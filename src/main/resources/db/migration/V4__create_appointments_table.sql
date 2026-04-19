CREATE TABLE appointments (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id      UUID        NOT NULL REFERENCES doctors(id),
    patient_id     UUID        NOT NULL REFERENCES patients(id),
    date_time      TIMESTAMP   NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'
                               CHECK (status IN ('SCHEDULED','COMPLETED','CANCELLED')),
    reason         TEXT        NOT NULL,
    cancel_reason  TEXT,
    created_at     TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_appointments_doctor_datetime  ON appointments(doctor_id, date_time);
CREATE INDEX idx_appointments_patient_datetime ON appointments(patient_id, date_time);
CREATE INDEX idx_appointments_status           ON appointments(status);
