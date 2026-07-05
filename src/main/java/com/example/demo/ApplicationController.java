package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ApplicationController {

    @Autowired
    private BankServiceRepository bankServiceRepository;

    @Autowired
    private LoanApplicationRepository loanApplicationRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/apply")
    public String showServices(Model model) {
        model.addAttribute("services", bankServiceRepository.findAll());
        return "apply";
    }

    @PostMapping("/apply")
    public String createApplication(@RequestParam("serviceId") int serviceId, Authentication auth, Model model) {

        User customer = userRepository.findByUsername(auth.getName());
        BankService service = bankServiceRepository.findById(serviceId).orElse(null);

        if (service == null) {
            model.addAttribute("error", "Invalid service selected.");
            model.addAttribute("services", bankServiceRepository.findAll());
            return "apply";
        }

        LoanApplication app = new LoanApplication();
        app.setCustomer(customer);
        app.setService(service);

        LoanApplication saved = loanApplicationRepository.save(app);

        return "redirect:/upload?applicationId=" + saved.getId();
    }

    @GetMapping("/my-applications")
    public String myApplications(Authentication auth, Model model) {
        User customer = userRepository.findByUsername(auth.getName());
        model.addAttribute("applications", loanApplicationRepository.findByCustomerId(customer.getId()));
        return "my_applications";
    }
}