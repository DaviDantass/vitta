package davidantass.vitta.domain.appointment;

import jakarta.persistence.*;
import davidantass.vitta.domain.doctor.Doctor;
import davidantass.vitta.domain.patient.Patient;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    private LocalDateTime dateTime;

    @Deprecated
    public Appointment(){}

    public Appointment(Doctor doctor, Patient patient, AppointmentForm form) {
        updateDetails(doctor, patient, form);
    }

    public void updateDetails(Doctor doctor, Patient patient, AppointmentForm form) {
        this.doctor = doctor;
        this.patient = patient;
        this.dateTime = form.dateTime();
    }

    public Long getId() {
        return id;
    }

    public Patient getPatient() {
        return patient;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

}
