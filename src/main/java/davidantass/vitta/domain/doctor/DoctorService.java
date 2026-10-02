package davidantass.vitta.domain.doctor;

import jakarta.transaction.Transactional;
import davidantass.vitta.domain.BusinessRuleException;
import davidantass.vitta.domain.user.UserService;
import davidantass.vitta.domain.user.Profile;
import davidantass.vitta.domain.appointment.AppointmentRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DoctorService {

    private final DoctorRepository repository;
    private final UserService userService;
    private final AppointmentRepository appointmentRepository;

    public DoctorService(DoctorRepository repository, UserService userService, AppointmentRepository appointmentRepository) {
        this.repository = repository;
        this.userService = userService;
        this.appointmentRepository = appointmentRepository;
    }

    public Page<DoctorSummary> list(Pageable pagination) {
        return repository.findAll(pagination).map(DoctorSummary::new);
    }

    @Transactional
    public void save(DoctorForm form) {
        if (repository.isAlreadyRegistered(form.email(), form.medicalRegistration(), form.id())) {
            throw new BusinessRuleException("Email or medical registration already registered for another doctor!");
        }

        if (form.id() == null) {
            Long id = userService.save(form.name(), form.email(), form.medicalRegistration(), Profile.DOCTOR);
            repository.saveAndFlush(new Doctor(id, form));
        } else {
            var doctor = repository.findById(form.id()).orElseThrow();
            userService.updateDetails(doctor.getId(), form.name(), form.email());
            doctor.updateDetails(form);
            repository.flush();
        }
    }

    public DoctorForm findById(Long id) {
        var doctor = repository.findById(id).orElseThrow();
        return new DoctorForm(doctor.getId(), doctor.getName(), doctor.getEmail(), doctor.getPhone(), doctor.getMedicalRegistration(), doctor.getSpecialty());
    }

    @Transactional
    public void delete(Long id) {
        var doctor = repository.findById(id).orElseThrow();
        if (appointmentRepository.existsByDoctorId(id)) {
            throw new BusinessRuleException("Cannot delete a doctor with registered appointments.");
        }
        repository.delete(doctor);
        // Delete the doctor before its user, respecting the shared-ID foreign key.
        repository.flush();
        userService.delete(id);
    }

    public List<DoctorSummary> listBySpecialty(Specialty specialty) {
        return repository.findBySpecialty(specialty).stream().map(DoctorSummary::new).toList();
    }

}
