package com.example.demo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/manager")
public class ManagerController {

    @Autowired
    private LoanApplicationRepository loanApplicationRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private EmailService emailService;

    @GetMapping("")
    public String managerDashboard(Authentication auth, Model model, HttpServletRequest request) {

        User manager = userRepository.findByUsername(auth.getName());

        List<LoanApplication> unassigned = loanApplicationRepository.findByStatusAndManagerIsNull("MANAGER_REVIEW");
        List<LoanApplication> myQueue = loanApplicationRepository.findByManagerIdAndStatus(manager.getId(), "MANAGER_REVIEW");

        model.addAttribute("unassignedApplications", unassigned);
        model.addAttribute("myQueue", myQueue);
        model.addAttribute("manager", manager);
        model.addAttribute("profilePic", manager.getProfileImage());

        auditLogService.log(auth.getName(), "MANAGER_DASHBOARD_OPENED",
                "Manager viewed final-decision queue | Unassigned: " + unassigned.size() + " | Mine: " + myQueue.size(),
                request);

        return "manager_dashboard";
    }

    @PostMapping("/claim/{id}")
    public String claimApplication(@PathVariable Integer id, Authentication auth, HttpServletRequest request) {

        LoanApplication app = loanApplicationRepository.findById(id).orElse(null);
        User manager = userRepository.findByUsername(auth.getName());

        if (app == null || app.getManager() != null || !"MANAGER_REVIEW".equals(app.getStatus())) {
            auditLogService.log(auth.getName(), "MANAGER_CLAIM_FAILED",
                    "Could not claim application #" + id + " (already claimed or wrong status)", request);
            return "redirect:/manager";
        }

        app.setManager(manager);
        loanApplicationRepository.save(app);

        String customerName = app.getCustomer() != null ? app.getCustomer().getUsername() : "unknown customer";
        auditLogService.log(auth.getName(), "APPLICATION_CLAIMED_BY_MANAGER",
                "Manager claimed application #" + app.getId() + " for " + customerName, request);

        return "redirect:/manager/review/" + id;
    }

    @GetMapping("/review/{id}")
    public String reviewApplication(@PathVariable Integer id, Authentication auth, Model model, HttpServletRequest request) {

        LoanApplication app = loanApplicationRepository.findById(id).orElse(null);
        if (app == null) {
            return "redirect:/manager";
        }

        User manager = userRepository.findByUsername(auth.getName());

        if (app.getManager() == null || app.getManager().getId() != manager.getId()) {
            auditLogService.log(auth.getName(), "MANAGER_REVIEW_DENIED",
                    "Manager attempted to review unclaimed/other-manager application #" + id, request);
            return "redirect:/manager";
        }

        List<Document> documents = documentRepository.findByApplicationId(id);

        model.addAttribute("loanApp", app);
        model.addAttribute("customer", app.getCustomer());
        model.addAttribute("documents", documents);
        model.addAttribute("profilePic", manager.getProfileImage());

        return "manager_review";
    }

    @PostMapping("/approve/{id}")
    public String approveApplication(@PathVariable Integer id, @RequestParam(required = false) String remarks,
            Authentication auth, HttpServletRequest request) {

        LoanApplication app = loanApplicationRepository.findById(id).orElse(null);
        User manager = userRepository.findByUsername(auth.getName());

        if (app == null || app.getManager() == null || app.getManager().getId() != manager.getId()) {
            return "redirect:/manager";
        }

        app.setStatus("APPROVED");
        app.setManagerRemarks(remarks);
        app.setManagerDecisionAt(LocalDateTime.now());
        loanApplicationRepository.save(app);

        String customerName = app.getCustomer() != null ? app.getCustomer().getUsername() : "unknown customer";
        auditLogService.log(auth.getName(), "APPLICATION_APPROVED",
                "Application #" + app.getId() + " (" + customerName + ") given FINAL APPROVAL by manager"
                        + (remarks != null && !remarks.isBlank() ? " | Remarks: " + remarks : ""),
                request);

        User customer = app.getCustomer();
        if (customer != null && customer.getEmail() != null && !customer.getEmail().isEmpty()) {
            emailService.sendApplicationApproved(
                    customer.getEmail(), "Application #" + app.getId(),
                    app.getService() != null ? app.getService().getServiceName() : "your requested service");

            auditLogService.log(auth.getName(), "CUSTOMER_NOTIFIED",
                    "Approval email sent to " + customer.getEmail() + " for application #" + app.getId(), request);
        }

        return "redirect:/manager";
    }

    @PostMapping("/reject/{id}")
    public String rejectApplication(@PathVariable Integer id, @RequestParam(required = false) String remarks,
            Authentication auth, HttpServletRequest request) {

        LoanApplication app = loanApplicationRepository.findById(id).orElse(null);
        User manager = userRepository.findByUsername(auth.getName());

        if (app == null || app.getManager() == null || app.getManager().getId() != manager.getId()) {
            return "redirect:/manager";
        }

        app.setStatus("REJECTED_BY_MANAGER");
        app.setManagerRemarks(remarks);
        app.setManagerDecisionAt(LocalDateTime.now());
        loanApplicationRepository.save(app);

        String customerName = app.getCustomer() != null ? app.getCustomer().getUsername() : "unknown customer";
        auditLogService.log(auth.getName(), "APPLICATION_REJECTED_BY_MANAGER",
                "Application #" + app.getId() + " (" + customerName + ") REJECTED by manager"
                        + (remarks != null && !remarks.isBlank() ? " | Reason: " + remarks : ""),
                request);

        User customer = app.getCustomer();
        if (customer != null && customer.getEmail() != null && !customer.getEmail().isEmpty()) {
            emailService.sendApplicationRejected(
                    customer.getEmail(), "Application #" + app.getId(),
                    app.getService() != null ? app.getService().getServiceName() : "your requested service",
                    remarks);

            auditLogService.log(auth.getName(), "CUSTOMER_NOTIFIED",
                    "Rejection email sent to " + customer.getEmail() + " for application #" + app.getId(), request);
        }

        return "redirect:/manager";
    }
}