package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class V9__link_patients_to_users extends BaseJavaMigration {
    // MySQL ALTER TABLE commits implicitly.
    @Override
    public boolean canExecuteInTransaction() {
        return false;
    }

    @Override
    public void migrate(Context context) throws Exception {
        var connection = context.getConnection();
        var patients = readPatients(connection);
        // Validate every existing account before changing data or foreign keys.
        for (var patient : patients) {
            patient.userId = findPatientAccount(connection, patient.email);
        }
        var encoder = new BCryptPasswordEncoder();
        for (var patient : patients) {
            if (patient.userId == null) {
                try (var insert = connection.prepareStatement(
                        "INSERT INTO users(name, email, password, profile) VALUES (?, ?, ?, 'PATIENT')",
                        Statement.RETURN_GENERATED_KEYS)) {
                    insert.setString(1, patient.name);
                    insert.setString(2, patient.email);
                    insert.setString(3, encoder.encode(patient.cpf));
                    insert.executeUpdate();
                    try (var keys = insert.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Unable to create legacy patient account.");
                        patient.userId = keys.getLong(1);
                    }
                }
            } else {
                try (var update = connection.prepareStatement("UPDATE users SET name = ?, email = ? WHERE id = ?")) {
                    update.setString(1, patient.name);
                    update.setString(2, patient.email);
                    update.setLong(3, patient.userId);
                    update.executeUpdate();
                }
            }
        }

        execute(connection, "ALTER TABLE appointments DROP FOREIGN KEY fk_appointments_patient_id");
        execute(connection, "ALTER TABLE patients MODIFY id BIGINT NOT NULL");
        // Temporary negative IDs avoid collisions while remapping both tables.
        execute(connection, "UPDATE patients SET id = -id");
        execute(connection, "UPDATE appointments SET patient_id = -patient_id");
        for (var patient : patients) {
            try (var update = connection.prepareStatement("UPDATE patients SET id = ? WHERE id = ?")) {
                update.setLong(1, patient.userId);
                update.setLong(2, -patient.id);
                update.executeUpdate();
            }
            try (var update = connection.prepareStatement("UPDATE appointments SET patient_id = ? WHERE patient_id = ?")) {
                update.setLong(1, patient.userId);
                update.setLong(2, -patient.id);
                update.executeUpdate();
            }
        }
        execute(connection, "ALTER TABLE patients ADD CONSTRAINT fk_patients_user_id FOREIGN KEY (id) REFERENCES users(id)");
        execute(connection, "ALTER TABLE appointments ADD CONSTRAINT fk_appointments_patient_id FOREIGN KEY (patient_id) REFERENCES patients(id)");
        execute(connection, "UPDATE users u JOIN doctors d ON d.id = u.id SET u.profile = 'DOCTOR'");
    }

    private List<LegacyPatient> readPatients(Connection connection) throws SQLException {
        var patients = new ArrayList<LegacyPatient>();
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT id, name, email, cpf FROM patients ORDER BY id")) {
            while (rows.next()) {
                var id = rows.getLong("id");
                if (id <= 0) throw new SQLException("Legacy patient IDs must be positive before migration.");
                patients.add(new LegacyPatient(id, rows.getString("name"), rows.getString("email"), rows.getString("cpf")));
            }
        }
        return patients;
    }

    private Long findPatientAccount(Connection connection, String email) throws SQLException {
        try (var query = connection.prepareStatement("""
                SELECT u.id, u.profile, d.id AS doctor_id
                FROM users u LEFT JOIN doctors d ON d.id = u.id
                WHERE LOWER(u.email) = LOWER(?)
                """)) {
            query.setString(1, email);
            try (var rows = query.executeQuery()) {
                if (!rows.next()) return null;
                if (!"PATIENT".equals(rows.getString("profile")) || rows.getObject("doctor_id") != null) {
                    throw new SQLException("A legacy patient email belongs to a non-patient account. Resolve this conflict before migration.");
                }
                return rows.getLong("id");
            }
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static class LegacyPatient {
        final long id;
        final String name;
        final String email;
        final String cpf;
        Long userId;

        LegacyPatient(long id, String name, String email, String cpf) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.cpf = cpf;
        }
    }
}
