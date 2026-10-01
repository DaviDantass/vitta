package davidantass.vitta.domain.appointment;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import davidantass.vitta.domain.doctor.Specialty;

import java.time.LocalDateTime;

public record AppointmentForm(

        Long id,
        @NotNull
        Long doctorId,

        @NotNull
        Long patientId,

        @NotNull
        @Future
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime dateTime,

        @NotNull
        Specialty specialty) {
}
