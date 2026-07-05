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
@RequestMapping("/officer")
public class OfficerController {

    @Autowired
    private LoanApplicationRepository loanApplicationRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("")
    public String officerDashboard(Authentication auth, Model model, HttpServletRequest request) {
        User officer = userRepository.findByUsername(auth.getName());

        List<LoanApplication> unassigned =
                loanApplicationRepository.findByStatusAndOfficerIsNull("UNDER_REVIEW");
        List<LoanApplication> myQueue =
                loanApplicationRepository.findByOfficerIdAndStatus(officer.getId(), "UNDER_REVIEW");

        model.addAttribute("unassignedApplications", unassigned);
        model.addAttribute("myQueue", myQueue);
        model.addAttribute("officer", officer);
        model.addAttribute("profilePic", officer.getProfileImage());

        auditLogService.log(auth.getName(), "OFFICER_DASHBOARD_OPENED",
                "Officer viewed review queue | Unassigned: " + unassigned.size() + " | Mine: " + myQueue.size(),
                request);

        return "officer_dashboard";
    }

    @PostMapping("/claim/{id}")
    public String claimApplication(@PathVariable Integer id, Authentication auth, HttpServletRequest request) {
        LoanApplication app = loanApplicationRepository.findById(id).orElse(null);
        User officer = userRepository.findByUsername(auth.getName());

        if (app == null || app.getOfficer() != null || !"UNDER_REVIEW".equals(app.getStatus())) {
            auditLogService.log(auth.getName(), "OFFICER_CLAIM_FAILED",
                    "Could not claim application #" + id + " (already claimed or wrong status)", request);
            return "redirect:/officer";
        }

        app.setOfficer(officer);
        loanApplicationRepository.save(app);

        String customerName = app.getCustomer() != null ? app.getCustomer().getUsername() : "unknown customer";
        auditLogService.log(auth.getName(), "APPLICATION_CLAIMED_BY_OFFICER",
                "Officer claimed application #" + app.getId() + " for " + customerName, request);

        return "redirect:/officer/review/" + id;
    }

    @GetMapping("/review/{id}")
    public String reviewApplication(@PathVariable Integer id, Authentication auth, Model model, HttpServletRequest request) {
        LoanApplication app = loanApplicationRepository.findById(id).orElse(null);
        if (app == null) {
            return "redirect:/officer";
        }

        User officer = userRepository.findByUsername(auth.getName());

        if (app.getOfficer() == null || app.getOfficer().getId() != officer.getId()) {
            auditLogService.log(auth.getName(), "OFFICER_REVIEW_DENIED",
                    "Officer attempted to review unclaimed/other-officer application #" + id, request);
            return "redirect:/officer";
        }

        List<Document> documents = documentRepository.findByApplicationId(id);

        model.addAttribute("loanApp", app);
        model.addAttribute("customer", app.getCustomer());
        model.addAttribute("documents", documents);
        model.addAttribute("profilePic", officer.getProfileImage());

        String customerName = app.getCustomer() != null ? app.getCustomer().getUsername() : "unknown customer";
        auditLogService.log(auth.getName(), "APPLICATION_REVIEWED",
                "Officer opened review for application #" + app.getId() + " (" + customerName + ")", request);

        return "officer_review";
    }

    @PostMapping("/approve/{id}")
    public String approveApplication(@PathVariable String id,
                                      @RequestParam(required = false) String remarks,
                                      Authentication auth,
                                      HttpServletRequest request) {

        Integer appId = parseId(id);
        if (appId == null) {
            auditLogService.log(auth.getName(), "OFFICER_APPROVE_BAD_ID",
                    "Approve attempted with invalid id: " + id, request);
            return "redirect:/officer";
        }

        LoanApplication app = loanApplicationRepository.findById(appId).orElse(null);
        User officer = userRepository.findByUsername(auth.getName());

        if (app == null || app.getOfficer() == null || app.getOfficer().getId() != officer.getId()) {
            return "redirect:/officer";
        }

        app.setStatus("MANAGER_REVIEW");
        app.setOfficerRemarks(remarks);
        app.setOfficerDecisionAt(LocalDateTime.now());
        loanApplicationRepository.save(app);

        String customerName = app.getCustomer() != null ? app.getCustomer().getUsername() : "unknown customer";
        auditLogService.log(auth.getName(), "APPLICATION_APPROVED_BY_OFFICER",
                "Application #" + app.getId() + " (" + customerName + ") approved by officer, forwarded to manager"
                        + (remarks != null && !remarks.isBlank() ? " | Remarks: " + remarks : ""),
                request);

        return "redirect:/officer";
    }

    @PostMapping("/reject/{id}")
    public String rejectApplication(@PathVariable String id,
                                     @RequestParam(required = false) String remarks,
                                     Authentication auth,
                                     HttpServletRequest request) {

        Integer appId = parseId(id);
        if (appId == null) {
            auditLogService.log(auth.getName(), "OFFICER_REJECT_BAD_ID",
                    "Reject attempted with invalid id: " + id, request);
            return "redirect:/officer";
        }

        LoanApplication app = loanApplicationRepository.findById(appId).orElse(null);
        User officer = userRepository.findByUsername(auth.getName());

        if (app == null || app.getOfficer() == null || app.getOfficer().getId() != officer.getId()) {
            return "redirect:/officer";
        }

        app.setStatus("REJECTED_BY_OFFICER");
        app.setOfficerRemarks(remarks);
        app.setOfficerDecisionAt(LocalDateTime.now());
        loanApplicationRepository.save(app);

        String customerName = app.getCustomer() != null ? app.getCustomer().getUsername() : "unknown customer";
        auditLogService.log(auth.getName(), "APPLICATION_REJECTED_BY_OFFICER",
                "Application #" + app.getId() + " (" + customerName + ") rejected by officer"
                        + (remarks != null && !remarks.isBlank() ? " | Reason: " + remarks : ""),
                request);

        return "redirect:/officer";
    }

    private Integer parseId(String raw) {
        if (raw == null || raw.isBlank() || "null".equalsIgnoreCase(raw)) return null;
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}