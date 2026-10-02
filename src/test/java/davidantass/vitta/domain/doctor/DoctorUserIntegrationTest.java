package davidantass.vitta.domain.doctor;

import davidantass.vitta.domain.BusinessRuleException;
import davidantass.vitta.domain.appointment.*;
import davidantass.vitta.domain.patient.*;
import davidantass.vitta.domain.user.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=${VITTA_TEST_DB_URL}",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false"
})
@EnabledIfEnvironmentVariable(named = "VITTA_TEST_DB_URL", matches = ".+")
@Transactional
class DoctorUserIntegrationTest {
    @Autowired DoctorService service;
    @Autowired DoctorRepository doctors;
    @Autowired UserService users;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired PatientService patients;
    @Autowired PatientRepository patientRepository;
    @Autowired AppointmentService appointments;
    @Autowired JdbcTemplate jdbc;

    private Doctor registerDoctor() {
        service.save(new DoctorForm(null, "Doctor", "sync-doctor@example.com", "123", "123456", Specialty.CARDIOLOGY));
        return doctors.findAll().getFirst();
    }

    @Test
    void registrationSharesTheIdAndHashesTheInitialPassword() {
        var doctor = registerDoctor();
        var user = userRepository.findById(doctor.getId()).orElseThrow();
        assertThat(user.getName()).isEqualTo(doctor.getName());
        assertThat(user.getEmail()).isEqualTo(doctor.getEmail());
        assertThat(passwordEncoder.matches("123456", user.getPassword())).isTrue();
        assertThat(jdbc.queryForObject("""
                SELECT REFERENCED_TABLE_NAME FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'doctors' AND COLUMN_NAME = 'id'
                AND REFERENCED_TABLE_NAME IS NOT NULL
                """, String.class)).isEqualTo("users");
        assertThat(jdbc.queryForObject("""
                SELECT EXTRA FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'doctors' AND COLUMN_NAME = 'id'
                """, String.class)).doesNotContain("auto_increment");
    }

    @Test
    void editingSynchronizesTheLoginAndNameWithoutResettingThePassword() {
        var doctor = registerDoctor();
        var originalPassword = userRepository.findById(doctor.getId()).orElseThrow().getPassword();
        service.save(new DoctorForm(doctor.getId(), "New name", "new-login@example.com", "456", "654321", Specialty.DERMATOLOGY));
        var user = userRepository.findById(doctor.getId()).orElseThrow();
        assertThat(user.getName()).isEqualTo("New name");
        assertThat(user.getEmail()).isEqualTo(doctor.getEmail()).isEqualTo("new-login@example.com");
        assertThat(user.getPassword()).isEqualTo(originalPassword);
        assertThat(users.loadUserByUsername("new-login@example.com").getUsername()).isEqualTo("new-login@example.com");
        assertThatThrownBy(() -> users.loadUserByUsername("sync-doctor@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void existingStandaloneUserEmailCannotBeUsedForADoctor() {
        users.save("Other user", "existing@example.com", "password", Profile.RECEPTIONIST);
        assertThatThrownBy(() -> service.save(new DoctorForm(null, "Doctor", "EXISTING@example.com", "123", "123456", Specialty.CARDIOLOGY)))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(doctors.count()).isZero();
    }

    @Test
    void conflictingEditDoesNotChangeEitherRecord() {
        var doctor = registerDoctor();
        users.save("Other user", "existing@example.com", "password", Profile.RECEPTIONIST);
        assertThatThrownBy(() -> service.save(new DoctorForm(doctor.getId(), "Changed", "existing@example.com", "123", "123456", Specialty.CARDIOLOGY)))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(doctor.getEmail()).isEqualTo("sync-doctor@example.com");
        var user = userRepository.findById(doctor.getId()).orElseThrow();
        assertThat(user.getEmail()).isEqualTo("sync-doctor@example.com");
        assertThat(user.getName()).isEqualTo("Doctor");
    }

    @Test
    void deletionRemovesBothRecordsInForeignKeyOrder() {
        var doctor = registerDoctor();
        service.delete(doctor.getId());
        userRepository.flush();
        assertThat(doctors.existsById(doctor.getId())).isFalse();
        assertThat(userRepository.existsById(doctor.getId())).isFalse();
    }

    @Test
    void missingDoctorCannotDeleteAStandaloneUser() {
        var userId = users.save("Standalone", "standalone@example.com", "password", Profile.RECEPTIONIST);
        assertThatThrownBy(() -> service.delete(userId)).isInstanceOf(NoSuchElementException.class);
        assertThat(userRepository.existsById(userId)).isTrue();
    }

    @Test
    void linkedAppointmentsBlockDeletionOfBothDoctorAndUser() {
        var doctor = registerDoctor();
        patients.save(new PatientForm(null, "Patient", "patient@example.com", "123", "123.456.789-00"));
        var patient = patientRepository.findAll().getFirst();
        appointments.save(new AppointmentForm(null, doctor.getId(), patient.getId(), LocalDateTime.now().plusDays(1), Specialty.CARDIOLOGY));
        assertThatThrownBy(() -> service.delete(doctor.getId())).isInstanceOf(BusinessRuleException.class);
        assertThat(doctors.existsById(doctor.getId())).isTrue();
        assertThat(userRepository.existsById(doctor.getId())).isTrue();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void failedDoctorInsertRollsBackTheNewUser() {
        assertThatThrownBy(() -> service.save(new DoctorForm(null, "Rollback doctor", "rollback-doctor@example.com", "1".repeat(21), "123456", Specialty.CARDIOLOGY)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(userRepository.findByEmailIgnoreCase("rollback-doctor@example.com")).isEmpty();
        assertThat(doctors.count()).isZero();
    }
}
