package davidantass.vitta.domain.patient;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END
            FROM Patient p
            WHERE (p.email = :email OR p.cpf = :cpf) AND (:id IS NULL OR p.id <> :id)
            """)
    boolean isAlreadyRegistered(String email, String cpf, Long id);
}
