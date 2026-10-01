package davidantass.vitta.domain.doctor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    @Query("""
            SELECT
                CASE WHEN COUNT(m) > 0 THEN TRUE ELSE FALSE END
            FROM
                Doctor m
            WHERE (m.email = :email OR m.medicalRegistration = :medicalRegistration) AND (:id IS NULL OR m.id <> :id)
            """)
    boolean isAlreadyRegistered(String email, String medicalRegistration, Long id);

    List<Doctor> findBySpecialty(Specialty specialty);

}
