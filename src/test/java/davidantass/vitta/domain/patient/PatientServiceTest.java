package davidantass.vitta.domain.patient;

import davidantass.vitta.domain.BusinessRuleException;
import davidantass.vitta.domain.appointment.AppointmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {
    @Mock PatientRepository repository;
    @Mock AppointmentRepository appointments;
    @InjectMocks PatientService service;

    private PatientForm form(Long id) {
        return new PatientForm(id, "Ana", "ana@example.com", "11999999999", "123.456.789-00");
    }

    @Test
    void editingUpdatesTheExistingEntityInsteadOfCreatingAnotherPatient() {
        var existing = new Patient(new PatientForm(null, "Old name", "old@example.com", "123", "123.456.789-00"));
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        service.save(form(7L));
        verify(repository).saveAndFlush(same(existing));
        assertThat(existing.getName()).isEqualTo("Ana");
        assertThat(existing.getEmail()).isEqualTo("ana@example.com");
        verify(repository).isAlreadyRegistered("ana@example.com", "123.456.789-00", 7L);
    }

    @Test
    void duplicateCpfOrEmailIsRejectedBeforeSaving() {
        when(repository.isAlreadyRegistered("ana@example.com", "123.456.789-00", null)).thenReturn(true);
        assertThatThrownBy(() -> service.save(form(null))).isInstanceOf(BusinessRuleException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void missingPatientCannotBeEdited() {
        assertThatThrownBy(() -> service.save(form(7L))).isInstanceOf(NoSuchElementException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void patientWithAppointmentsCannotBeDeleted() {
        when(repository.findById(7L)).thenReturn(Optional.of(new Patient(form(null))));
        when(appointments.existsByPatientId(7L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(7L)).isInstanceOf(BusinessRuleException.class);
        verify(repository, never()).delete(any(Patient.class));
    }

    @Test
    void patientWithoutAppointmentsCanBeDeleted() {
        var patient = new Patient(form(null));
        when(repository.findById(7L)).thenReturn(Optional.of(patient));
        service.delete(7L);
        verify(repository).delete(patient);
        verify(repository).flush();
    }
}
