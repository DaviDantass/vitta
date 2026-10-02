package davidantass.vitta.domain.appointment;

import davidantass.vitta.domain.BusinessRuleException;
import davidantass.vitta.domain.doctor.*;
import davidantass.vitta.domain.patient.*;
import davidantass.vitta.domain.user.UserService;
import davidantass.vitta.domain.user.Profile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=${VITTA_TEST_DB_URL}",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false"
})
@EnabledIfEnvironmentVariable(named = "VITTA_TEST_DB_URL", matches = ".+")
@Transactional
class PatientAppointmentIntegrationTest {
    @Autowired PatientService patients;
    @Autowired PatientRepository patientRepository;
    @Autowired AppointmentService appointments;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired DoctorRepository doctors;
    @Autowired UserService users;
    @Autowired JdbcTemplate jdbc;

    private Patient registerPatient() {
        patients.save(new PatientForm(null, "Ana", "integration@example.com", "11999999999", "123.456.789-00"));
        return patientRepository.findAll().getFirst();
    }

    @Test
    void editingPreservesThePatientIdAndCount() {
        var patient = registerPatient();
        patients.save(new PatientForm(patient.getId(), "Ana edited", patient.getEmail(), "123", patient.getCpf()));
        assertThat(patientRepository.count()).isEqualTo(1);
        assertThat(patients.findById(patient.getId()).name()).isEqualTo("Ana edited");
    }

    @Test
    void uniquenessAllowsOwnDetailsButRejectsAnotherPatientsCpfOrEmail() {
        var patient = registerPatient();
        patients.save(patients.findById(patient.getId()));
        assertThatThrownBy(() -> patients.save(new PatientForm(null, "Other", "other@example.com", "123", patient.getCpf())))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> patients.save(new PatientForm(null, "Other", patient.getEmail(), "123", "987.654.321-00")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void appointmentPersistsBothForeignKeysAndPreventsPatientDeletion() {
        var patient = registerPatient();
        var userId = users.save("Doctor", "doctor@example.com", "123456", Profile.DOCTOR);
        var doctor = doctors.saveAndFlush(new Doctor(userId, new DoctorForm(null, "Doctor", "doctor@example.com", "123", "123456", Specialty.CARDIOLOGY)));
        appointments.save(new AppointmentForm(null, doctor.getId(), patient.getId(), LocalDateTime.now().plusDays(1), Specialty.CARDIOLOGY));
        var appointment = appointmentRepository.findAll().getFirst();
        assertThat(appointments.findById(appointment.getId()).patientId()).isEqualTo(patient.getId());
        assertThat(jdbc.queryForObject("SELECT patient_id FROM appointments WHERE id = ?", Long.class, appointment.getId())).isEqualTo(patient.getId());
        assertThat(jdbc.queryForObject("SELECT doctor_id FROM appointments WHERE id = ?", Long.class, appointment.getId())).isEqualTo(doctor.getId());
        assertThatThrownBy(() -> patients.delete(patient.getId())).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void migratedSchemaHasBothForeignKeysAndNoLegacyPatientText() {
        assertThat(jdbc.queryForList("""
                SELECT COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appointments'
                AND REFERENCED_TABLE_NAME IS NOT NULL
                """, String.class)).containsExactlyInAnyOrder("doctor_id", "patient_id");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appointments' AND COLUMN_NAME = 'patient'
                """, Integer.class)).isZero();
    }
}
