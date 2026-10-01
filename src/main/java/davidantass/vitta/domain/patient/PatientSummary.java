package davidantass.vitta.domain.patient;

public record PatientSummary(
        Long id,
        String name,
        String email,
        String phone,
        String cpf
) {
    public PatientSummary(Patient patient) {
        this(patient.getId(), patient.getName(), patient.getEmail(), patient.getPhone(), patient.getCpf());
    }
}
