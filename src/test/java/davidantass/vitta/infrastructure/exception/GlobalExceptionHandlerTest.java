package davidantass.vitta.infrastructure.exception;

import davidantass.vitta.controller.DoctorController;
import davidantass.vitta.controller.PatientController;
import davidantass.vitta.domain.BusinessRuleException;
import davidantass.vitta.domain.doctor.DoctorService;
import davidantass.vitta.domain.patient.PatientService;
import davidantass.vitta.domain.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({DoctorController.class, PatientController.class})
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {
    @Autowired MockMvc mvc;
    @MockBean DoctorService doctors;
    @MockBean PatientService patients;
    @MockBean UserService users;

    @Test
    void missingEntityReturns404() throws Exception {
        when(doctors.findById(7L)).thenThrow(new EntityNotFoundException("Database details"));
        mvc.perform(get("/doctors/form").param("id", "7"))
                .andExpect(status().isNotFound()).andExpect(view().name("error/404"))
                .andExpect(content().string(not(containsString("Database details"))));
    }

    @Test
    void unknownRouteReturns404() throws Exception {
        mvc.perform(get("/missing-page"))
                .andExpect(status().isNotFound()).andExpect(view().name("error/404"));
    }

    @Test
    void invalidIdReturns400() throws Exception {
        mvc.perform(get("/patients/form").param("id", "invalid"))
                .andExpect(status().isBadRequest()).andExpect(view().name("error/request"));
        verifyNoInteractions(patients);
    }

    @Test
    void missingRequiredParameterReturns400() throws Exception {
        mvc.perform(delete("/patients"))
                .andExpect(status().isBadRequest()).andExpect(view().name("error/request"));
        verifyNoInteractions(patients);
    }

    @Test
    void invalidSpecialtyReturns400InsteadOf500() throws Exception {
        mvc.perform(get("/doctors/UNKNOWN"))
                .andExpect(status().isBadRequest()).andExpect(view().name("error/request"));
        verifyNoInteractions(doctors);
    }

    @Test
    void uncaughtBusinessRuleReturns422AndEscapesItsMessage() throws Exception {
        doThrow(new BusinessRuleException("Cannot delete <doctor> with appointments")).when(doctors).delete(7L);
        mvc.perform(delete("/doctors").param("id", "7"))
                .andExpect(status().isUnprocessableEntity()).andExpect(view().name("error/request"))
                .andExpect(content().string(containsString("Cannot delete &lt;doctor&gt; with appointments")));
    }

    @Test
    void databaseConflictReturns409WithoutExposingSql() throws Exception {
        doThrow(new DataIntegrityViolationException("INSERT INTO users password=secret")).when(doctors).delete(7L);
        mvc.perform(delete("/doctors").param("id", "7"))
                .andExpect(status().isConflict()).andExpect(view().name("error/request"))
                .andExpect(content().string(not(containsString("password=secret"))));
    }

    @Test
    void accessDeniedReturns403() throws Exception {
        doThrow(new AccessDeniedException("Internal details")).when(doctors).delete(7L);
        mvc.perform(delete("/doctors").param("id", "7"))
                .andExpect(status().isForbidden()).andExpect(view().name("error/request"));
    }

    @Test
    void unsupportedMethodPreserves405AndAllowHeader() throws Exception {
        mvc.perform(patch("/doctors"))
                .andExpect(status().isMethodNotAllowed()).andExpect(header().string("Allow", containsString("GET")))
                .andExpect(view().name("error/request"));
    }

    @Test
    void unexpectedErrorReturns500WithoutExposingTheException() throws Exception {
        when(doctors.list(any())).thenThrow(new IllegalStateException("Internal failure details"));
        mvc.perform(get("/doctors"))
                .andExpect(status().isInternalServerError()).andExpect(view().name("error/500"))
                .andExpect(content().string(not(containsString("Internal failure details"))));
    }
}
