package davidantass.vitta.controller;

import davidantass.vitta.domain.user.PasswordChangeForm;
import davidantass.vitta.domain.user.User;
import davidantass.vitta.domain.user.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
    public String change(@ModelAttribute("form") PasswordChangeForm form,
                         @AuthenticationPrincipal User user) {
        if (!userService.changePassword(user, form.currentPassword(), form.newPassword(), form.newPasswordConfirmation())) {
            return "redirect:/change-password?error";
        }
        return "redirect:/?passwordChanged";
    }
}
