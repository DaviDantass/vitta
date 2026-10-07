package davidantass.vitta.controller;

import davidantass.vitta.domain.BusinessRuleException;
import davidantass.vitta.domain.appointment.AppointmentService;
import davidantass.vitta.domain.patient.*;
import davidantass.vitta.domain.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({PatientController.class, AppointmentController.class})
@AutoConfigureMockMvc(addFilters = false)
class PatientAndAppointmentControllerTest {
    @Autowired MockMvc mvc;
    @MockBean PatientService patients;
    @MockBean AppointmentService appointments;
    @MockBean UserService users;

    @Test
    void emptyPatientListRenders() throws Exception {
        when(patients.list(any())).thenReturn(new PageImpl<>(List.of()));
        mvc.perform(get("/patients")).andExpect(status().isOk())
                .andExpect(content().string(containsString("No records found.")));
    }

    @Test
    void populatedPatientListRendersActionsAndPagination() throws Exception {
        var patient = new PatientSummary(7L, "Ana", "ana@example.com", "11999999999", "123.456.789-00");
        when(patients.list(any())).thenReturn(new PageImpl<>(List.of(patient), PageRequest.of(0, 10), 11));
        mvc.perform(get("/patients")).andExpect(status().isOk())
                .andExpect(content().string(containsString("patients?page=1")))
                .andExpect(content().string(containsString("123.456.789-00")));
    }

    @Test
    void registrationAndEditingFormsRender() throws Exception {
        mvc.perform(get("/patients/form")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Patient registration")));
        when(patients.findById(7L)).thenReturn(new PatientForm(7L, "Ana", "ana@example.com", "123", "123.456.789-00"));
        mvc.perform(get("/patients/form").param("id", "7")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Edit patient")))
                .andExpect(content().string(containsString("value=\"Ana\"")));
    }

    @Test
    void invalidPatientReturnsFormWithoutSaving() throws Exception {
        mvc.perform(post("/patients").param("name", "Ana").param("email", "invalid")
                        .param("phone", "123").param("cpf", "invalid"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "email", "cpf"));
        verify(patients, never()).save(any());
    }

    @Test
    void validPatientRedirectsAfterSaving() throws Exception {
        mvc.perform(post("/patients").param("name", "Ana").param("email", "ana@example.com")
                        .param("phone", "123").param("cpf", "123.456.789-00"))
                .andExpect(redirectedUrl("/patients?success"));
        verify(patients).save(any());
    }

    @Test
    void duplicateRegistrationPreservesEnteredData() throws Exception {
        doThrow(new BusinessRuleException("CPF already registered")).when(patients).save(any());
        mvc.perform(post("/patients").param("name", "Ana").param("email", "ana@example.com")
                        .param("phone", "123").param("cpf", "123.456.789-00"))
                .andExpect(status().isOk()).andExpect(model().attribute("error", "CPF already registered"))
                .andExpect(content().string(containsString("value=\"Ana\"")));
    }

    @Test
    void linkedPatientDeletionRedirectsWithError() throws Exception {
        doThrow(new BusinessRuleException("Patient has appointments")).when(patients).delete(7L);
        mvc.perform(delete("/patients").param("id", "7"))
                .andExpect(redirectedUrl("/patients"))
                .andExpect(flash().attribute("error", "Patient has appointments"));
    }

    @Test
    void missingPatientReturns404() throws Exception {
        when(patients.findById(7L)).thenThrow(new NoSuchElementException());
        mvc.perform(get("/patients/form").param("id", "7"))
                .andExpect(status().isNotFound()).andExpect(view().name("error/404"));
    }

    @Test
    void appointmentFormDisplaysRegisteredPatients() throws Exception {
        when(patients.listForAppointments()).thenReturn(List.of(new PatientSummary(7L, "Ana", "ana@example.com", "123", "123.456.789-00")));
        mvc.perform(get("/appointments/form")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Ana — 123.456.789-00")))
                .andExpect(content().string(containsString("name=\"patientId\"")));
    }

    @Test
    void invalidAppointmentKeepsPatientOptionsAndDoesNotSave() throws Exception {
        when(patients.listForAppointments()).thenReturn(List.of(new PatientSummary(7L, "Ana", "ana@example.com", "123", "123.456.789-00")));
        mvc.perform(post("/appointments")).andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "doctorId", "patientId", "dateTime", "specialty"))
                .andExpect(content().string(containsString("Ana — 123.456.789-00")));
        verify(appointments, never()).save(any());
    }
}
