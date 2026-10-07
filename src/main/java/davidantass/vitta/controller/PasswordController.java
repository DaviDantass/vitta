package davidantass.vitta.controller;

import davidantass.vitta.domain.user.PasswordChangeForm;
import davidantass.vitta.domain.user.User;
import davidantass.vitta.domain.user.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

@Controller
public class PasswordController {
    private final UserService userService;

    public PasswordController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/change-password")
    public String showForm(Model model) {
        model.addAttribute("form", new PasswordChangeForm("", "", ""));
        return "authentication/change-password";
    }

    @PostMapping("/change-password")
    public String change(@Valid @ModelAttribute("form") PasswordChangeForm form,
                         BindingResult result,
                         @AuthenticationPrincipal User user,
                         Model model) {
        if (result.hasErrors()
                || !userService.changePassword(user, form.currentPassword(), form.newPassword(), form.newPasswordConfirmation())) {
            model.addAttribute("passwordError", "Unable to change password. Check the supplied values.");
            return "authentication/change-password";
        }
        return "redirect:/?passwordChanged";
    }
}
