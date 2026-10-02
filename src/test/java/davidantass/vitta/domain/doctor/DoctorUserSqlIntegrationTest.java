package davidantass.vitta.domain.doctor;

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
class DoctorUserSqlIntegrationTest {
    @Autowired DoctorService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;

    private static final String SELECT_DOCTOR_AND_USER = """
            SELECT d.id AS doctor_id, u.id AS user_id,
                   d.name AS doctor_name, u.name AS user_name,
                   d.email AS doctor_email, u.email AS user_email,
                   d.medical_registration, u.password, u.profile
            FROM doctors d
            JOIN users u ON u.id = d.id
            WHERE d.id = ?
            """;

    // No test transaction: each service call commits before the independent SELECT.
    @Test
    @DisplayName("Cadastro, edição e exclusão de médico conferidos por SELECT no MySQL")
    void doctorLifecycleIsConsistentInBothTablesAfterCommit() {
        var suffix = UUID.randomUUID().toString();
        var email = "doctor-sql-" + suffix + "@example.com";
        var editedEmail = "edited-" + suffix + "@example.com";
        try {
            service.save(new DoctorForm(null, "Doctor SQL", email, "11999999999", "999001", Specialty.CARDIOLOGY));
            var id = jdbc.queryForObject("SELECT id FROM doctors WHERE email = ?", Long.class, email);
            var registered = selectDoctorAndUser(id);

            assertThat(registered.doctorId()).isEqualTo(registered.userId()).isEqualTo(id);
            assertThat(registered.doctorName()).isEqualTo(registered.userName()).isEqualTo("Doctor SQL");
            assertThat(registered.doctorEmail()).isEqualTo(registered.userEmail()).isEqualTo(email);
            assertThat(registered.profile()).isEqualTo("DOCTOR");
            assertThat(registered.registration()).isEqualTo("999001");
            assertThat(registered.password()).isNotEqualTo("999001");
            assertThat(passwordEncoder.matches("999001", registered.password())).isTrue();
            assertThat(countDoctors(id)).isEqualTo(1);
            assertThat(countUsers(id)).isEqualTo(1);
            System.out.printf("SELECT cadastro: doctor_id=%d, user_id=%d, profile=DOCTOR, nome/email iguais, senha BCrypt OK%n", id, registered.userId());

            service.save(new DoctorForm(id, "Doctor SQL edited", editedEmail, "11888888888", "999002", Specialty.DERMATOLOGY));
            var edited = selectDoctorAndUser(id);

            assertThat(edited.doctorId()).isEqualTo(id);
            assertThat(edited.userId()).isEqualTo(id);
            assertThat(edited.doctorName()).isEqualTo(edited.userName()).isEqualTo("Doctor SQL edited");
            assertThat(edited.doctorEmail()).isEqualTo(edited.userEmail()).isEqualTo(editedEmail);
            assertThat(edited.profile()).isEqualTo("DOCTOR");
            assertThat(edited.registration()).isEqualTo("999002");
            assertThat(edited.password()).isEqualTo(registered.password());
            assertThat(countDoctors(id)).isEqualTo(1);
            assertThat(countUsers(id)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email)).isZero();
            System.out.printf("SELECT edição: doctor_id=%d, user_id=%d, nome/email sincronizados, senha preservada%n", id, edited.userId());

            service.delete(id);
            assertThat(countDoctors(id)).isZero();
            assertThat(countUsers(id)).isZero();
            System.out.println("SELECT exclusão: doctors=0, users=0 para o ID cadastrado");
        } finally {
            // Clean only this test's random emails, even if an assertion fails.
            jdbc.update("DELETE FROM doctors WHERE email IN (?, ?)", email, editedEmail);
            jdbc.update("DELETE FROM users WHERE email IN (?, ?)", email, editedEmail);
        }
    }

    private DoctorUserRow selectDoctorAndUser(Long id) {
        return jdbc.queryForObject(SELECT_DOCTOR_AND_USER, (row, index) -> new DoctorUserRow(
                row.getLong("doctor_id"), row.getLong("user_id"),
                row.getString("doctor_name"), row.getString("user_name"),
                row.getString("doctor_email"), row.getString("user_email"),
                row.getString("medical_registration"), row.getString("password"), row.getString("profile")), id);
    }

    private int countDoctors(Long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM doctors WHERE id = ?", Integer.class, id);
    }

    private int countUsers(Long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, id);
    }

    private record DoctorUserRow(long doctorId, long userId, String doctorName, String userName,
                                String doctorEmail, String userEmail, String registration, String password, String profile) {}
}
