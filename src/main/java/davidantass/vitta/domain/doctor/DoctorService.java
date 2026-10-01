package davidantass.vitta.domain.doctor;

import jakarta.transaction.Transactional;
import davidantass.vitta.domain.BusinessRuleException;
import davidantass.vitta.domain.user.UserService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DoctorService {

    private final DoctorRepository repository;
    private final UserService userService;

    public DoctorService(DoctorRepository repository, UserService userService) {
        this.repository = repository;
        this.userService = userService;
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
            repository.save(new Doctor(form));
            userService.saveUser(form.name(), form.email(), form.crm());
        } else {
            var doctor = repository.findById(form.id()).orElseThrow();
            doctor.updateDetails(form);
        }
    }

    public DoctorForm findById(Long id) {
        var doctor = repository.findById(id).orElseThrow();
        return new DoctorForm(doctor.getId(), doctor.getName(), doctor.getEmail(), doctor.getPhone(), doctor.getMedicalRegistration(), doctor.getSpecialty());
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<DoctorSummary> listBySpecialty(Specialty specialty) {
        return repository.findBySpecialty(specialty).stream().map(DoctorSummary::new).toList();
    }

}
