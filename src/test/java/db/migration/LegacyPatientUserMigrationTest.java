package db.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "VITTA_LEGACY_TEST_DB_URL", matches = ".+")
class LegacyPatientUserMigrationTest {
    @Test
    void legacyPatientIdsAndAppointmentReferencesAreRemappedWithoutCollisions() {
        var url = System.getenv("VITTA_LEGACY_TEST_DB_URL");
        var username = System.getenv().getOrDefault("DB_USERNAME", "vitta");
        var password = System.getenv().getOrDefault("DB_PASSWORD", "vitta_local");
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(url, username, password));
        // This test resets only an explicitly named, exclusive legacy test database.
        assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class)).startsWith("vitta_profile_legacy_test_");
        var before = Flyway.configure().dataSource(url, username, password)
                .locations("classpath:db/migration").target("8").cleanDisabled(false).load();
        before.clean();
        try {
            before.migrate();
            jdbc.update("INSERT INTO users(id,name,email,password,profile) VALUES (1,'Legacy doctor','legacy-doctor@example.com','unchanged-hash','RECEPTIONIST')");
            jdbc.update("INSERT INTO doctors(id,name,email,phone,medical_registration,specialty) VALUES (1,'Legacy doctor','legacy-doctor@example.com','123','123456','CARDIOLOGY')");
            jdbc.update("INSERT INTO patients(id,name,email,phone,cpf) VALUES (1,'Patient one','one@example.com','123','123.456.789-00'),(2,'Patient two','two@example.com','123','987.654.321-00')");
            jdbc.update("INSERT INTO appointments(doctor_id,patient_id,date_time) VALUES (1,1,'2027-01-01 10:00:00'),(1,2,'2027-01-01 11:00:00')");

            Flyway.configure().dataSource(url, username, password).locations("classpath:db/migration").target("9").load().migrate();

            assertThat(jdbc.queryForList("SELECT id FROM patients ORDER BY id", Long.class)).containsExactly(2L, 3L);
            assertThat(jdbc.queryForList("SELECT p.name FROM appointments a JOIN patients p ON p.id=a.patient_id ORDER BY a.id", String.class))
                    .containsExactly("Patient one", "Patient two");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM patients p JOIN users u ON u.id=p.id WHERE p.name=u.name AND p.email=u.email AND u.profile='PATIENT'", Integer.class)).isEqualTo(2);
            var patientHash = jdbc.queryForObject("SELECT password FROM users WHERE email='one@example.com'", String.class);
            assertThat(new BCryptPasswordEncoder().matches("123.456.789-00", patientHash)).isTrue();
            assertThat(jdbc.queryForObject("SELECT profile FROM users WHERE id=1", String.class)).isEqualTo("DOCTOR");
            assertThat(jdbc.queryForObject("SELECT password FROM users WHERE id=1", String.class)).isEqualTo("unchanged-hash");
            System.out.println("SELECT migração: pacientes 1/2 → usuários 2/3, consultas preservadas, perfis PATIENT/DOCTOR corretos");
        } finally {
            before.clean();
        }
    }
}
