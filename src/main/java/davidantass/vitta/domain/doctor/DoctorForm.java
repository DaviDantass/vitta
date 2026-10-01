package davidantass.vitta.domain.doctor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record DoctorForm(
        Long id,
        @NotBlank
        String name,
        @NotBlank
        @Email
        String email,
        @NotBlank
        String phone,
        @NotBlank
        @Pattern(regexp = "\\d{4,6}", message = "Medical registration must contain 4 to 6 digits")
        String medicalRegistration,
        @NotNull
        Specialty specialty
) {}

