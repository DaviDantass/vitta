package davidantass.vitta.domain.appointment;

import org.springframework.transaction.annotation.Transactional;
import davidantass.vitta.domain.BusinessRuleException;
import davidantass.vitta.domain.patient.PatientRepository;
import davidantass.vitta.domain.user.Profile;
import davidantass.vitta.domain.user.User;
import davidantass.vitta.domain.doctor.DoctorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
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
    public Page<AppointmentSummary> list(Pageable pagination, User loggedUser) {
        if (loggedUser.getProfile() == Profile.RECEPTIONIST) {
            return repository.findAllByOrderByDateTimeAscIdAsc(pagination).map(AppointmentSummary::new);
        }
        return repository.findPersonalizedAppointments(loggedUser.getId(), pagination).map(AppointmentSummary::new);
    }

    @Transactional
    public void save(AppointmentForm form, User loggedUser) {
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
            var appointment = findAuthorized(form.id(), loggedUser);
            appointment.updateDetails(appointmentDoctor, appointmentPatient, form);
        }
    }

    // Kept for existing service-level tests that create new appointments directly.
    @Transactional
    public void save(AppointmentForm form) {
        if (form.id() != null) {
            throw new AccessDeniedException("An authenticated user is required to update an appointment.");
        }
        save(form, null);
    }

    @Transactional(readOnly = true)
    public AppointmentForm findById(Long id, User loggedUser) {
        var appointment = findAuthorized(id, loggedUser);
        return new AppointmentForm(appointment.getId(), appointment.getDoctor().getId(), appointment.getPatient().getId(), appointment.getDateTime(), appointment.getDoctor().getSpecialty());
    }

    // Kept package-private for domain tests; web requests use the authorized overload above.
    @Transactional(readOnly = true)
    AppointmentForm findById(Long id) {
        var appointment = repository.findById(id).orElseThrow();
        return new AppointmentForm(appointment.getId(), appointment.getDoctor().getId(), appointment.getPatient().getId(), appointment.getDateTime(), appointment.getDoctor().getSpecialty());
    }

    @Transactional
    public void delete(Long id, User loggedUser) {
        var appointment = findAuthorized(id, loggedUser);
        repository.delete(appointment);
    }

    private Appointment findAuthorized(Long id, User loggedUser) {
        var appointment = loggedUser.getProfile() == Profile.RECEPTIONIST
                ? repository.findById(id)
                : repository.findAuthorizedById(id, loggedUser.getId());

        if (appointment.isEmpty()) {
            throw new AccessDeniedException("You do not have access to this appointment.");
        }
        return appointment.get();
    }

}
