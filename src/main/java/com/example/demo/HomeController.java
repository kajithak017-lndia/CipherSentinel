package com.example.demo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private UserService userService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private AnomalyRepository anomalyRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }
    @GetMapping("/dashboard")
    public String dashboard(
        Authentication auth, Model model) {

        User user = userRepository
            .findByUsername(auth.getName());

        // Get ONLY current user's documents
        List<Document> allDocs = documentRepository
            .findByUploadedBy(user.getId());

        // ===== STATUS COUNTS =====
        long safeCount = allDocs.stream()
            .filter(d -> "SAFE"
                .equals(d.getStatus())).count();
        long unsafeCount = allDocs.stream()
            .filter(d -> "UNSAFE"
                .equals(d.getStatus())).count();
        long pendingCount = allDocs.stream()
            .filter(d -> "PENDING"
                .equals(d.getStatus())).count();

        // ===== LAND RECORD COUNTS =====
        long landSafe = allDocs.stream()
            .filter(d -> "LAND_RECORD"
                .equals(d.getDocumentType()) &&
                "SAFE".equals(d.getStatus()))
            .count();
        long landUnsafe = allDocs.stream()
            .filter(d -> "LAND_RECORD"
                .equals(d.getDocumentType()) &&
                "UNSAFE".equals(d.getStatus()))
            .count();

        // ===== LEGAL DOCUMENT COUNTS =====
        long legalSafe = allDocs.stream()
            .filter(d -> "LEGAL_DOCUMENT"
                .equals(d.getDocumentType()) &&
                "SAFE".equals(d.getStatus()))
            .count();
        long legalUnsafe = allDocs.stream()
            .filter(d -> "LEGAL_DOCUMENT"
                .equals(d.getDocumentType()) &&
                "UNSAFE".equals(d.getStatus()))
            .count();

        // ===== FINANCIAL COUNTS =====
        long financialSafe = allDocs.stream()
            .filter(d -> "FINANCIAL_STATEMENT"
                .equals(d.getDocumentType()) &&
                "SAFE".equals(d.getStatus()))
            .count();
        long financialUnsafe = allDocs.stream()
            .filter(d -> "FINANCIAL_STATEMENT"
                .equals(d.getDocumentType()) &&
                "UNSAFE".equals(d.getStatus()))
            .count();

        // ===== TOTAL TYPE COUNTS =====
        long landCount = allDocs.stream()
            .filter(d -> "LAND_RECORD"
                .equals(d.getDocumentType()))
            .count();
        long legalCount = allDocs.stream()
            .filter(d -> "LEGAL_DOCUMENT"
                .equals(d.getDocumentType()))
            .count();
        long financialCount = allDocs.stream()
            .filter(d -> "FINANCIAL_STATEMENT"
                .equals(d.getDocumentType()))
            .count();

        // ===== ANOMALY COUNTS (user only) =====
        List<Anomaly> userAnomalies =
            new java.util.ArrayList<>();
        for (Document doc : allDocs) {
            userAnomalies.addAll(
                anomalyRepository
                    .findByDocumentId(doc.getId()));
        }

        long totalAnomalies = userAnomalies.size();
        long highCount = userAnomalies.stream()
            .filter(a -> "HIGH"
                .equals(a.getSeverity())).count();
        long mediumCount = userAnomalies.stream()
            .filter(a -> "MEDIUM"
                .equals(a.getSeverity())).count();
        long lowCount = userAnomalies.stream()
            .filter(a -> "LOW"
                .equals(a.getSeverity())).count();

        long auditCount = auditLogRepository
            .findByUserId(user.getId()).size();

        // ===== ADD TO MODEL =====
        model.addAttribute("totalDocs",
            allDocs.size());
        model.addAttribute("safeCount", safeCount);
        model.addAttribute("unsafeCount", unsafeCount);
        model.addAttribute("pendingCount", pendingCount);

        // Chart data
        model.addAttribute("landSafe", landSafe);
        model.addAttribute("landUnsafe", landUnsafe);
        model.addAttribute("legalSafe", legalSafe);
        model.addAttribute("legalUnsafe", legalUnsafe);
        model.addAttribute("financialSafe",
            financialSafe);
        model.addAttribute("financialUnsafe",
            financialUnsafe);

        model.addAttribute("landCount", landCount);
        model.addAttribute("legalCount", legalCount);
        model.addAttribute("financialCount",
            financialCount);
        model.addAttribute("totalAnomalies",
            totalAnomalies);
        model.addAttribute("highCount", highCount);
        model.addAttribute("mediumCount", mediumCount);
        model.addAttribute("lowCount", lowCount);
        model.addAttribute("auditCount", auditCount);
        return "dashboard";
    }
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
        @RequestParam("username") String username,
        @RequestParam("email") String email,
        @RequestParam("password") String password,
        @RequestParam("securityQuestion")
            String securityQuestion,
        @RequestParam("securityAnswer")
            String securityAnswer,
        Model model) {

        // Check if username & email id exists
    	User existingUsername =
    		    userRepository.findByUsername(username);

    		if(existingUsername != null) {
    		    model.addAttribute(
    		        "error",
    		        "Username already exists!");
    		    return "register";
    		}

    		User existingEmail =
    		    userRepository.findByEmail(email);

    		if(existingEmail != null) {
    		    model.addAttribute(
    		        "error",
    		        "Email already registered!");
    		    return "register";
    		}

        // ✅ ALWAYS assign USER role on register
        // Only admin can change role later!
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole("USER");
        user.setEmail(email);

        user.setSecurityQuestion(
            securityQuestion);

        user.setSecurityAnswer(
            securityAnswer);
        userService.saveUser(user);
        return "redirect:/login?registered";
    }

    @GetMapping("/profile")
    public String profile(
        Authentication auth,
        Model model,
        HttpServletRequest request) {

        User user = userRepository
            .findByUsername(auth.getName());

        List<Document> docs = documentRepository
            .findByUploadedBy(user.getId());

        long safeCount = docs.stream()
            .filter(d -> "SAFE"
                .equals(d.getStatus())).count();
        long unsafeCount = docs.stream()
            .filter(d -> "UNSAFE"
                .equals(d.getStatus())).count();
        long pendingCount = docs.stream()
            .filter(d -> "PENDING"
                .equals(d.getStatus())).count();

        model.addAttribute("user", user);
        model.addAttribute("totalDocs", docs.size());
        model.addAttribute("safeCount", safeCount);
        model.addAttribute("unsafeCount", unsafeCount);
        model.addAttribute("pendingCount", pendingCount);

        // ✅ Use getProfileImage() — matches your User.java
        model.addAttribute("profilePic",
            user.getProfileImage());

        return "profile";
    }

    @GetMapping("/settings")
    public String settings(
        Authentication auth, Model model) {
        User user = userRepository
            .findByUsername(auth.getName());
        model.addAttribute("user", user);
        return "settings";
    }
    
    @PostMapping("/settings/update")
    @ResponseBody
    public String updateSettings(
            @RequestParam String type,
            @RequestParam boolean value,
            Authentication auth) {

        User user =
            userRepository.findByUsername(
                auth.getName());

        switch(type) {

            case "audit":
                user.setAuditNotifications(value);
                break;

            case "upload":
                user.setUploadNotifications(value);
                break;

            case "anomaly":
                user.setAnomalyAlerts(value);
                break;
        }

        userRepository.save(user);

        return "OK";
    }
    @GetMapping("/change-password")
    public String changePasswordPage() {
        return "change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(
        Authentication auth,
        @RequestParam("currentPassword")
            String currentPassword,
        @RequestParam("newPassword")
            String newPassword,
        @RequestParam("confirmPassword")
            String confirmPassword,
        HttpServletRequest request,
        Model model) {

        User user = userRepository
            .findByUsername(auth.getName());

        if (!passwordEncoder.matches(
            currentPassword, user.getPassword())) {
            model.addAttribute("error",
                "Current password is incorrect!");
            return "change-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error",
                "New passwords do not match!");
            return "change-password";
        }

        if (newPassword.length() < 6) {
            model.addAttribute("error",
                "Password must be at least " +
                "6 characters!");
            return "change-password";
        }

        user.setPassword(
            passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // ✅ LOG PASSWORD CHANGE
        auditLogService.log(
            auth.getName(),
            "PASSWORD_CHANGED",
            "User changed their password",
            request);

        model.addAttribute("success",
            "Password changed successfully!");
        return "change-password";
    }

    @GetMapping("/deactivate")
    public String deactivateAccount(
        Authentication auth,
        HttpServletRequest request) {

        User user = userRepository
            .findByUsername(auth.getName());

        // ✅ LOG DEACTIVATION
        auditLogService.log(
            auth.getName(),
            "ACCOUNT_DEACTIVATED",
            "User deactivated their account",
            request);

        user.setRole("DEACTIVATED");
        userRepository.save(user);

        return "redirect:/logout";
    }

    @GetMapping("/audit")
    public String viewAudit(
        Authentication auth, Model model) {

        User user = userRepository
            .findByUsername(auth.getName());

        model.addAttribute("logs",
            auditLogRepository
                .findByUserId(user.getId()));

        return "audit";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    

    // Profile photo remove
    @PostMapping("/profile/remove-photo")
    @ResponseBody
    public String removePhoto(
        Authentication auth) {

        User user = userRepository
            .findByUsername(auth.getName());
        user.setProfileImage(null);
        userRepository.save(user);
        return "removed";
    }
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(

        @RequestParam("username") String username,

        @RequestParam("email") String email,

        @RequestParam("securityQuestion")
        String securityQuestion,

        @RequestParam("securityAnswer")
        String securityAnswer,

        @RequestParam("newPassword")
        String newPassword,

        @RequestParam("confirmPassword")
        String confirmPassword,

        Model model) {

        User user =
            userRepository.findByUsername(username);

        // Username check
        if (user == null) {

            model.addAttribute(
                "error",
                "Username not found!");

            return "forgot-password";
        }

        // Email check
        if (!user.getEmail()
                .equalsIgnoreCase(email)) {

            model.addAttribute(
                "error",
                "Email does not match!");

            return "forgot-password";
        }

        // Security Question check
        if (!user.getSecurityQuestion()
                .equals(securityQuestion)) {

            model.addAttribute(
                "error",
                "Security question does not match!");

            return "forgot-password";
        }

        // Security Answer check
        if (!user.getSecurityAnswer()
                .equalsIgnoreCase(securityAnswer)) {

            model.addAttribute(
                "error",
                "Security answer incorrect!");

            return "forgot-password";
        }

        // Password match check
        if (!newPassword
                .equals(confirmPassword)) {

            model.addAttribute(
                "error",
                "Passwords do not match!");

            return "forgot-password";
        }

        // Password length check
        if (newPassword.length() < 6) {

            model.addAttribute(
                "error",
                "Password must be at least 6 characters!");

            return "forgot-password";
        }

        // Update password
        user.setPassword(
            passwordEncoder.encode(newPassword));

        userRepository.save(user);

        model.addAttribute(
            "success",
            "Password reset successfully! Please login.");

        return "forgot-password";
    }
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}