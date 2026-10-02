package davidantass.vitta.domain.doctor;

import jakarta.persistence.*;

@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String medicalRegistration;
    @Enumerated(EnumType.STRING)
    private Specialty specialty;

    @Deprecated
    public Doctor(){}

    public Doctor(Long id, DoctorForm form) {
        this.id = id;
        updateDetails(form);
    }

    public void updateDetails(DoctorForm form) {
        this.name = form.name();
        this.email = form.email();
        this.phone = form.phone();
        this.medicalRegistration = form.medicalRegistration();
        this.specialty = form.specialty();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getMedicalRegistration() {
        return medicalRegistration;
    }

    public Specialty getSpecialty() {
        return specialty;
    }

}

