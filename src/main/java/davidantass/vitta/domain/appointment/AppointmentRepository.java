package davidantass.vitta.domain.appointment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @EntityGraph(attributePaths = {"doctor", "patient"})
    Page<Appointment> findAllByOrderByDateTimeAscIdAsc(Pageable pagination);

    //Busque as consultas em que o usuário é o médico OU o paciente, já carregando médico e paciente junto, e pagine o resultado.
    @EntityGraph(attributePaths = {"doctor", "patient"})
    @Query("SELECT a FROM Appointment a WHERE (a.doctor.id = :userId OR a.patient.id = :userId) ORDER BY a.dateTime ASC, a.id ASC")
    Page<Appointment> findByUserId(@Param("userId") Long userId, Pageable pagination);

    boolean existsByPatientId(Long patientId);

    boolean existsByDoctorId(Long doctorId);

    @Query("SELECT c FROM Appointment c " + "WHERE (c.doctor.id = :id OR c.patient.id = :id)" + " ORDER BY c.dateTime ASC, c.id ASC")
    Page<Appointment> findPersonalizedAppointments(Long id, Pageable pagination);

}
