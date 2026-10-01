package davidantass.vitta.domain.patient;

import davidantass.vitta.domain.BusinessRuleException;
import davidantass.vitta.domain.appointment.AppointmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.List;
import org.springframework.data.domain.Sort;

@Service
public class PatientService {
    private final PatientRepository repository;
    private final AppointmentRepository appointmentRepository;

    public PatientService(PatientRepository repository, AppointmentRepository appointmentRepository) {
        this.repository = repository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(readOnly = true)
    public List<PatientSummary> listForAppointments() {
        return repository.findAll(Sort.by("name", "id")).stream().map(PatientSummary::new).toList();
    }

    @Transactional(readOnly = true)
    public Page<PatientSummary> list(Pageable pagination) {
        return repository.findAll(pagination).map(PatientSummary::new);
    }
    
    @Transactional(readOnly = true)
    public PatientForm findById(Long id) {
        var patient = findPatient(id);
        return new PatientForm(patient.getId(), patient.getName(), patient.getEmail(), patient.getPhone(), patient.getCpf());
    }

    @Transactional
    public void save(PatientForm form) {
        var patient = form.id() == null ? new Patient(form) : findPatient(form.id());
        if (repository.isAlreadyRegistered(form.email(), form.cpf(), form.id())) {
            throw new BusinessRuleException("Email or CPF already registered for another patient!");
        }
        patient.updateDetails(form);
        repository.saveAndFlush(patient);
    }

    @Transactional
    public void delete(Long id) {
        var patient = findPatient(id);
        if (appointmentRepository.existsByPatientId(id)) {
            throw new BusinessRuleException("Cannot delete a patient with registered appointments.");
        }
        repository.delete(patient);
        repository.flush();
    }

    private Patient findPatient(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Patient not found: " + id));
    }
}
