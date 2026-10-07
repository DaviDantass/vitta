package davidantass.vitta.controller;

import jakarta.validation.Valid;
import davidantass.vitta.domain.BusinessRuleException;
import davidantass.vitta.domain.appointment.AppointmentService;
import davidantass.vitta.domain.appointment.AppointmentForm;
import davidantass.vitta.domain.doctor.Specialty;
import davidantass.vitta.domain.patient.PatientService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import davidantass.vitta.domain.user.User;

@Controller
@RequestMapping("appointments")
public class AppointmentController {

    private static final String LIST_VIEW = "appointment/appointment-list";
    private static final String FORM_VIEW = "appointment/appointment-form";
    private static final String LIST_REDIRECT = "redirect:/appointments?success";

    private final AppointmentService service;
    private final PatientService patientService;

    public AppointmentController(AppointmentService appointmentService, PatientService patientService){
        this.service = appointmentService;
        this.patientService = patientService;
    }

    @ModelAttribute("specialties")
    public Specialty[] specialties() {
        return Specialty.values();
    }

    @GetMapping
    public String showList(@PageableDefault Pageable pagination, Model model, @AuthenticationPrincipal User loggedUser) {
        var appointments = service.list(pagination, loggedUser);
        model.addAttribute("appointments", appointments);
        return LIST_VIEW;
    }

    @GetMapping("form")
    public String showForm(Long id, Model model, @AuthenticationPrincipal User loggedUser) {
        model.addAttribute("patients", patientService.listForAppointments());
        if (id != null) {
            model.addAttribute("form", service.findById(id, loggedUser));
        } else {
            model.addAttribute("form", new AppointmentForm(null, null, null, null, null));
        }

        return FORM_VIEW;
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") AppointmentForm form, BindingResult result, Model model,
                       @AuthenticationPrincipal User loggedUser) {
        model.addAttribute("patients", patientService.listForAppointments());
        if (result.hasErrors()) {
            model.addAttribute("form", form);
            return FORM_VIEW;
        }

        try {
            service.save(form, loggedUser);
            return LIST_REDIRECT;
        } catch (BusinessRuleException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("form", form);
            return FORM_VIEW;
        }
    }

    @DeleteMapping
    public String delete(Long id, @AuthenticationPrincipal User loggedUser) {
        service.delete(id, loggedUser);
        return LIST_REDIRECT;
    }

}
