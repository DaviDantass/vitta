package davidantass.vitta.domain.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetForm(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 72) String newPassword,
        @NotBlank String newPasswordConfirmation
) {}
