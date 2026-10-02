package davidantass.vitta.domain.patient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=${VITTA_TEST_DB_URL}",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false"
})
@EnabledIfEnvironmentVariable(named = "VITTA_TEST_DB_URL", matches = ".+")
class PatientUserSqlIntegrationTest {
    @Autowired PatientService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;

    private static final String SELECT_PATIENT_AND_USER = """
            SELECT d.id AS patient_id, u.id AS user_id,
                   d.name AS patient_name, u.name AS user_name,
                   d.email AS patient_email, u.email AS user_email,
                   d.cpf, u.password, u.profile
            FROM patients d
            JOIN users u ON u.id = d.id
            WHERE d.id = ?
            """;

    // No test transaction: each service call commits before the independent SELECT.
    @Test
    @DisplayName("Cadastro, edição e exclusão de paciente conferidos por SELECT no MySQL")
    void patientLifecycleIsConsistentInBothTablesAfterCommit() {
        var suffix = UUID.randomUUID().toString();
        var email = "patient-sql-" + suffix + "@example.com";
        var editedEmail = "edited-" + suffix + "@example.com";
        try {
            service.save(new PatientForm(null, "Patient SQL", email, "11999999999", "123.456.789-00"));
            var id = jdbc.queryForObject("SELECT id FROM patients WHERE email = ?", Long.class, email);
            var registered = selectPatientAndUser(id);

            assertThat(registered.patientId()).isEqualTo(registered.userId()).isEqualTo(id);
            assertThat(registered.patientName()).isEqualTo(registered.userName()).isEqualTo("Patient SQL");
            assertThat(registered.patientEmail()).isEqualTo(registered.userEmail()).isEqualTo(email);
            assertThat(registered.profile()).isEqualTo("PATIENT");
            assertThat(registered.cpf()).isEqualTo("123.456.789-00");
            assertThat(registered.password()).isNotEqualTo("123.456.789-00");
            assertThat(passwordEncoder.matches("123.456.789-00", registered.password())).isTrue();
            assertThat(countPatients(id)).isEqualTo(1);
            assertThat(countUsers(id)).isEqualTo(1);
            System.out.printf("SELECT cadastro: patient_id=%d, user_id=%d, profile=PATIENT, nome/email iguais, senha BCrypt OK%n", id, registered.userId());

            service.save(new PatientForm(id, "Patient SQL edited", editedEmail, "11888888888", "987.654.321-00"));
            var edited = selectPatientAndUser(id);

            assertThat(edited.patientId()).isEqualTo(id);
            assertThat(edited.userId()).isEqualTo(id);
            assertThat(edited.patientName()).isEqualTo(edited.userName()).isEqualTo("Patient SQL edited");
            assertThat(edited.patientEmail()).isEqualTo(edited.userEmail()).isEqualTo(editedEmail);
            assertThat(edited.profile()).isEqualTo("PATIENT");
            assertThat(edited.cpf()).isEqualTo("987.654.321-00");
            assertThat(edited.password()).isEqualTo(registered.password());
            assertThat(countPatients(id)).isEqualTo(1);
            assertThat(countUsers(id)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email)).isZero();
            System.out.printf("SELECT edição: patient_id=%d, user_id=%d, nome/email sincronizados, senha preservada%n", id, edited.userId());

            service.delete(id);
            assertThat(countPatients(id)).isZero();
            assertThat(countUsers(id)).isZero();
            System.out.println("SELECT exclusão: patients=0, users=0 para o ID cadastrado");
        } finally {
            // Clean only this test's random emails, even if an assertion fails.
            jdbc.update("DELETE FROM patients WHERE email IN (?, ?)", email, editedEmail);
            jdbc.update("DELETE FROM users WHERE email IN (?, ?)", email, editedEmail);
        }
    }

    private PatientUserRow selectPatientAndUser(Long id) {
        return jdbc.queryForObject(SELECT_PATIENT_AND_USER, (row, index) -> new PatientUserRow(
                row.getLong("patient_id"), row.getLong("user_id"),
                row.getString("patient_name"), row.getString("user_name"),
                row.getString("patient_email"), row.getString("user_email"),
                row.getString("cpf"), row.getString("password"), row.getString("profile")), id);
    }

    private int countPatients(Long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM patients WHERE id = ?", Integer.class, id);
    }

    private int countUsers(Long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, id);
    }

    private record PatientUserRow(long patientId, long userId, String patientName, String userName,
                                String patientEmail, String userEmail, String cpf, String password, String profile) {}
}
