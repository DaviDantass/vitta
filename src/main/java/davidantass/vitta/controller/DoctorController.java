package davidantass.vitta.controller;

import jakarta.validation.Valid;
import davidantass.vitta.domain.BusinessRuleException;
import davidantass.vitta.domain.doctor.DoctorForm;
import davidantass.vitta.domain.doctor.DoctorSummary;
import davidantass.vitta.domain.doctor.Specialty;
import davidantass.vitta.domain.doctor.DoctorService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("doctors")
public class DoctorController {

    private static final String LIST_VIEW = "doctor/doctor-list";
    private static final String FORM_VIEW = "doctor/doctor-form";
    private static final String LIST_REDIRECT = "redirect:/doctors?success";

    private final DoctorService service;

    public DoctorController(DoctorService service) {
        this.service = service;
    }

    @ModelAttribute("specialties")
    public Specialty[] specialties() {
        return Specialty.values();
    }

    @GetMapping
    public String showList(@PageableDefault Pageable pagination, Model model) {
        var doctors = service.list(pagination);
        model.addAttribute("doctors", doctors);
        return LIST_VIEW;
    }

    @GetMapping("form")
    public String showForm(Long id, Model model) {
        if (id != null) {
            model.addAttribute("form", service.findById(id));
        } else {
            model.addAttribute("form", new DoctorForm(null, "", "", "", "", null));
        }

        return FORM_VIEW;
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") DoctorForm form, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("form", form);
            return FORM_VIEW;
        }

        try {
            service.save(form);
            return LIST_REDIRECT;
        } catch (BusinessRuleException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("form", form);
            return FORM_VIEW;
        }
    }

    @DeleteMapping
    public String delete(Long id) {
        service.delete(id);
        return LIST_REDIRECT;
    }

    @GetMapping("{specialty}")
    @ResponseBody
    public List<DoctorSummary> listDoctorsBySpecialty(@PathVariable Specialty specialty) {
        return service.listBySpecialty(specialty);
    }

}
