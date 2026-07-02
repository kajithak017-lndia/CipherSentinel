package com.example.demo;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoanApplicationController {

    @Autowired
    private BankServiceRepository bankServiceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoanApplicationService loanApplicationService;

    @GetMapping("/apply")

    public String apply(Model model) {

        List<BankService> services =
                bankServiceRepository.findAll();

        model.addAttribute(
                "services",
                services);

        return "apply_service";
    }
    @GetMapping("/customer/dashboard")
    public String customerDashboard() {

        return "customer_dashboard";

    }
    @PostMapping("/application/create")
    public String createApplication(
            @RequestParam Integer serviceId,
            Authentication authentication) {

    	User customer =
    	        userRepository.findByUsername(authentication.getName());

    	if (customer == null) {
    	    throw new RuntimeException("Customer not found");
    	}
        LoanApplication application =
                loanApplicationService
                .createApplication(
                        customer.getId(),
                        serviceId);

        return "redirect:/application/upload/"
                + application.getId();
    }
    @GetMapping("/my-applications")
    public String myApplications(Authentication authentication,
                                 Model model) {

        User customer =
                userRepository.findByUsername(authentication.getName());

        model.addAttribute(
                "applications",
                loanApplicationService.getCustomerApplications(customer.getId()));

        return "my_applications";
    }
    @GetMapping("/application/{id}")
    public String applicationDetails(@PathVariable Integer id,
                                     Model model) {

        model.addAttribute(
                "application",
                loanApplicationService.getApplication(id));

        return "application_details";
    }

}