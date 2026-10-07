package davidantass.vitta.controller;

import davidantass.vitta.domain.user.PasswordResetForm;
import davidantass.vitta.domain.user.PasswordResetRequest;
import davidantass.vitta.domain.user.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class PasswordResetController {
    private final UserService userService;
    private final String siteUrl;

    public PasswordResetController(UserService userService,
                                   @Value("${app.public-url:http://localhost:8080}") String siteUrl) {
        this.userService = userService;
        this.siteUrl = siteUrl;
    }

    @GetMapping("/forgot-password")
    public String showRequestForm(Model model) {
        model.addAttribute("form", new PasswordResetRequest(""));
        return "authentication/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String request(@Valid @ModelAttribute("form") PasswordResetRequest form,
                          BindingResult result, Model model) {
        if (!result.hasErrors()) {
            userService.requestPasswordReset(form.email(), siteUrl);
        }
        model.addAttribute("submitted", true);
        return "authentication/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetForm(@RequestParam String token, Model model) {
        model.addAttribute("form", new PasswordResetForm(token, "", ""));
        return "authentication/reset-password";
    }

    @PostMapping("/reset-password")
    public String reset(@Valid @ModelAttribute("form") PasswordResetForm form,
                        BindingResult result, Model model) {
        if (result.hasErrors() || !userService.resetPassword(form.token(), form.newPassword(), form.newPasswordConfirmation())) {
            model.addAttribute("error", "The reset link is invalid or the password data is not valid.");
            return "authentication/reset-password";
        }
        return "redirect:/login?passwordReset";
    }
}
