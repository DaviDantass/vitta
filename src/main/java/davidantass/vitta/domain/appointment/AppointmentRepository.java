package davidantass.vitta.domain.appointment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @EntityGraph(attributePaths = {"doctor", "patient"})
    Page<Appointment> findAllByOrderByDateTimeAscIdAsc(Pageable pagination);

    boolean existsByPatientId(Long patientId);

    boolean existsByDoctorId(Long doctorId);

}
