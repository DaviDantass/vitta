package davidantass.vitta.domain.doctor;

public record DoctorSummary(Long id, String name, String email, String medicalRegistration, Specialty specialty) {

    public DoctorSummary(Doctor doctor) {
        this(doctor.getId(), doctor.getName(), doctor.getEmail(), doctor.getMedicalRegistration(), doctor.getSpecialty());
    }

}

