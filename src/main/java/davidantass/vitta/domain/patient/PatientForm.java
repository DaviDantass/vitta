package davidantass.vitta.domain.patient;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PatientForm (
        Long id,
        @NotBlank
        @Size(max = 100, message = "Name must have at most 100 characters.")
        String name,
        @NotBlank
        @Email
        @Size(max = 100, message = "Email must have at most 100 characters.")
        String email,
        @NotBlank
        @Size(max = 20, message = "Phone must have at most 20 characters.")
        String phone,
        @NotBlank
        @Pattern( regexp = "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}",
                message = "CPF deve estar no formato 000.000.000-00")
        String cpf
) {
    
}
