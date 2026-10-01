package davidantass.vitta.domain.appointment;

import davidantass.vitta.domain.doctor.Specialty;

import java.time.LocalDateTime;

public record AppointmentSummary(Long id, String doctor, String patient, String patientCpf, LocalDateTime dateTime, Specialty specialty) {

    public AppointmentSummary(Appointment appointment) {
        this(appointment.getId(), appointment.getDoctor().getName(), appointment.getPatient().getName(), appointment.getPatient().getCpf(), appointment.getDateTime(), appointment.getDoctor().getSpecialty());
    }

}
