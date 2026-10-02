package davidantass.vitta.domain.doctor;

import davidantass.vitta.domain.BusinessRuleException;
import davidantass.vitta.domain.appointment.AppointmentRepository;
import davidantass.vitta.domain.user.UserService;
import davidantass.vitta.domain.user.Profile;
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
class DoctorServiceTest {
    @Mock DoctorRepository repository;
    @Mock UserService users;
    @Mock AppointmentRepository appointments;
    @InjectMocks DoctorService service;

    private DoctorForm form(Long id) {
        return new DoctorForm(id, "Doctor", "doctor@example.com", "123", "123456", Specialty.CARDIOLOGY);
    }

    @Test
    void registrationUsesTheUserIdForTheDoctor() {
        when(users.save("Doctor", "doctor@example.com", "123456", Profile.DOCTOR)).thenReturn(7L);
        service.save(form(null));
        verify(repository).saveAndFlush(argThat(doctor -> doctor.getId().equals(7L)));
    }

    @Test
    void editingSynchronizesTheUserAndExistingDoctor() {
        var doctor = new Doctor(7L, form(7L));
        when(repository.findById(7L)).thenReturn(Optional.of(doctor));
        var edit = new DoctorForm(7L, "Changed", "changed@example.com", "456", "654321", Specialty.DERMATOLOGY);
        service.save(edit);
        verify(users).updateDetails(7L, "Changed", "changed@example.com");
        assertThat(doctor.getName()).isEqualTo("Changed");
        assertThat(doctor.getEmail()).isEqualTo("changed@example.com");
        verify(users, never()).save(anyString(), anyString(), anyString(), any(Profile.class));
    }

    @Test
    void missingDoctorDoesNotDeleteAnUnrelatedUser() {
        assertThatThrownBy(() -> service.delete(7L)).isInstanceOf(NoSuchElementException.class);
        verifyNoInteractions(users);
        verify(repository, never()).delete(any(Doctor.class));
    }

    @Test
    void doctorWithAppointmentsCannotBeDeleted() {
        when(repository.findById(7L)).thenReturn(Optional.of(new Doctor(7L, form(7L))));
        when(appointments.existsByDoctorId(7L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(7L)).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(users);
        verify(repository, never()).delete(any(Doctor.class));
    }

    @Test
    void doctorIsDeletedBeforeTheUser() {
        var doctor = new Doctor(7L, form(7L));
        when(repository.findById(7L)).thenReturn(Optional.of(doctor));
        service.delete(7L);
        var order = inOrder(repository, users);
        order.verify(repository).delete(doctor);
        order.verify(repository).flush();
        order.verify(users).delete(7L);
    }
}
