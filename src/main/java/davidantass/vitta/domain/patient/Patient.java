package davidantass.vitta.domain.patient;

import jakarta.persistence.*;

@Entity
@Table(name = "patients")
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String cpf;

    public Patient() {}
    
    public Patient(PatientForm form) {
        updateDetails(form);
    }

    public void updateDetails(PatientForm form) {
        this.name = form.name();
        this.email = form.email();
        this.phone = form.phone();
        this.cpf = form.cpf();
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
    public String getCpf() {
        return cpf;
    }

}
