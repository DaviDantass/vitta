package davidantass.vitta.domain.appointment;

import org.springframework.transaction.annotation.Transactional;
import davidantass.vitta.domain.BusinessRuleException;
import davidantass.vitta.domain.patient.PatientRepository;
import davidantass.vitta.domain.user.Profile;
import davidantass.vitta.domain.user.User;
import davidantass.vitta.domain.doctor.DoctorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;

@Service
public class AppointmentService {

    private final AppointmentRepository repository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    public AppointmentService(AppointmentRepository repository, DoctorRepository doctorRepository, PatientRepository patientRepository) {
        this.repository = repository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    @Transactional(readOnly = true)
    public Page<AppointmentSummary> list(Pageable pagination, @AuthenticationPrincipal User loggedUser) {
        if (loggedUser.getProfile() == Profile.RECEPTIONIST) {
            return repository.findAllByOrderByDateTimeAscIdAsc(pagination).map(AppointmentSummary::new);
        }
        return repository.findPersonalizedAppointments(loggedUser.getId(), pagination).map(AppointmentSummary::new);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentSummary> listForUser(Long userId, Pageable pagination) {
        return repository.findByUserId(userId, pagination).map(AppointmentSummary::new);
    }

    @Transactional
    public void save(AppointmentForm form) {
        var appointmentDoctor = doctorRepository.findById(form.doctorId())
                .orElseThrow(() -> new BusinessRuleException("Selected doctor is no longer available."));
        var appointmentPatient = patientRepository.findById(form.patientId())
                .orElseThrow(() -> new BusinessRuleException("Selected patient is no longer available."));
        if (appointmentDoctor.getSpecialty() != form.specialty()) {
            throw new BusinessRuleException("Selected doctor does not match the specialty.");
        }
        if (form.id() == null) {
            repository.save(new Appointment(appointmentDoctor, appointmentPatient, form));
        } else {
            var appointment = repository.findById(form.id()).orElseThrow();
            appointment.updateDetails(appointmentDoctor, appointmentPatient, form);
        }
    }

    @Transactional(readOnly = true)
    public AppointmentForm findById(Long id) {
        var appointment = repository.findById(id).orElseThrow();
        return new AppointmentForm(appointment.getId(), appointment.getDoctor().getId(), appointment.getPatient().getId(), appointment.getDateTime(), appointment.getDoctor().getSpecialty());
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

}
