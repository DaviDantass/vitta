package davidantass.vitta.controller;

import davidantass.vitta.domain.BusinessRuleException;
import davidantass.vitta.domain.patient.PatientForm;
import davidantass.vitta.domain.patient.PatientService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/patients")
public class PatientController {
    private static final String LIST_VIEW = "patient/patient-list";
    private static final String FORM_VIEW = "patient/patient-form";
    private static final String LIST_REDIRECT = "redirect:/patients?success";

    private final PatientService service;

    public PatientController(PatientService service) {
        this.service = service;
    }

    @GetMapping
    public String showList(@PageableDefault(sort = {"name", "id"}) Pageable pagination, Model model) {
        model.addAttribute("patients", service.list(pagination));
        return LIST_VIEW;
    }

    @GetMapping("/form")
    public String showForm(@RequestParam(required = false) Long id, Model model) {
        model.addAttribute("form", id == null ? new PatientForm(null, "", "", "", "") : service.findById(id));
        return FORM_VIEW;
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") PatientForm form, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            service.save(form);
            return LIST_REDIRECT;
        } catch (BusinessRuleException e) {
            model.addAttribute("error", e.getMessage());
            return FORM_VIEW;
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("error", "Unable to save patient. Check whether the email or CPF is already registered.");
            return FORM_VIEW;
        }
    }

    @DeleteMapping
    public String delete(@RequestParam Long id, RedirectAttributes attributes) {
        try {
            service.delete(id);
            return LIST_REDIRECT;
        } catch (BusinessRuleException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            attributes.addFlashAttribute("error", "Cannot delete a patient with registered appointments.");
        }
        return "redirect:/patients";
    }
}
