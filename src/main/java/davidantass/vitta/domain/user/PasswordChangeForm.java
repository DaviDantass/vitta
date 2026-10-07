package davidantass.vitta.domain.user;

public record PasswordChangeForm(String currentPassword, String newPassword, String newPasswordConfirmation) {
}
