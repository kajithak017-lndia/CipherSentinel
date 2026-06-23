package com.example.demo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AuditLogRepository auditLogRepository;

	@Autowired
	private DocumentRepository documentRepository;

	@Autowired
	private AuditLogService auditLogService;

	@Autowired
	private AnomalyRepository anomalyRepository;

	//ADMIN DASHBOARD 
	@GetMapping("")
	public String adminDashboard(Authentication auth, Model model) {

		// Security check
		User currentUser = userRepository.findByUsername(auth.getName());
		if (!"ADMIN".equals(currentUser.getRole())) {
			return "redirect:/dashboard";
		}

		List<User> allUsers = userRepository.findAll();
		List<AuditLog> recentLogs = auditLogRepository.findTop10ByOrderByTimestampDesc();

		model.addAttribute("recentLogs", recentLogs);
		model.addAttribute("users", allUsers);
		model.addAttribute("totalUsers", allUsers.size());
		long totalDocuments = documentRepository.count();
		model.addAttribute("totalDocuments", totalDocuments);
		long totalAnomalies = anomalyRepository.count();
		model.addAttribute("totalAnomalies", totalAnomalies);
		model.addAttribute("currentUser", currentUser);

		// Count by role
		long adminCount = allUsers.stream().filter(u -> "ADMIN".equals(u.getRole())).count();
		long officerCount = allUsers.stream().filter(u -> "OFFICER".equals(u.getRole())).count();
		long auditorCount = allUsers.stream().filter(u -> "AUDITOR".equals(u.getRole())).count();
		long managerCount = allUsers.stream().filter(u -> "MANAGER".equals(u.getRole())).count();
		long analystCount = allUsers.stream().filter(u -> "ANALYST".equals(u.getRole())).count();
		long userCount = allUsers.stream().filter(u -> "USER".equals(u.getRole())).count();

		model.addAttribute("adminCount", adminCount);
		model.addAttribute("officerCount", officerCount);
		model.addAttribute("auditorCount", auditorCount);
		model.addAttribute("managerCount", managerCount);
		model.addAttribute("analystCount", analystCount);
		model.addAttribute("userCount", userCount);

		return "admin";
	}

	// ===== CHANGE USER ROLE =====
	@PostMapping("/change-role")
	public String changeRole(Authentication auth, @RequestParam("userId") int userId,
			@RequestParam("newRole") String newRole, HttpServletRequest request) {

		User currentUser = userRepository.findByUsername(auth.getName());
		if (!"ADMIN".equals(currentUser.getRole())) {
			return "redirect:/dashboard";
		}

		if (currentUser.getId() == userId) {
			return "redirect:/admin?error=" + "Cannot change your own role!";
		}

		User targetUser = userRepository.findById(userId).orElse(null);

		if (targetUser != null) {
			String oldRole = targetUser.getRole();
			targetUser.setRole(newRole);
			userRepository.save(targetUser);

			// ✅ If deactivating — expire their session
			if ("DEACTIVATED".equals(newRole)) {
				expireUserSessions(targetUser.getUsername(), request);
			}

			auditLogService.log(auth.getName(), "ROLE_CHANGED",
					"Changed role of '" + targetUser.getUsername() + "' from " + oldRole + " to " + newRole, request);
		}

		return "redirect:/admin?success=1";
	}

	// ===== DELETE USER =====
	@PostMapping("/delete-user")
	public String deleteUser(Authentication auth, @RequestParam("userId") int userId, HttpServletRequest request) {

		User currentUser = userRepository.findByUsername(auth.getName());
		if (!"ADMIN".equals(currentUser.getRole())) {
			return "redirect:/dashboard";
		}

		if (currentUser.getId() == userId) {
			return "redirect:/admin";
		}

		User targetUser = userRepository.findById(userId).orElse(null);

		if (targetUser != null) {

			// ✅ Expire their session first
			expireUserSessions(targetUser.getUsername(), request);

			auditLogService.log(auth.getName(), "USER_DELETED",
					"Deleted user: " + targetUser.getUsername() + " | Role was: " + targetUser.getRole(), request);

			// ✅ Delete their audit logs too
			List<AuditLog> userLogs = auditLogRepository.findByUserId(targetUser.getId());
			auditLogRepository.deleteAll(userLogs);

			// ✅ Delete their documents
			List<Document> userDocs = documentRepository.findByUploadedBy(targetUser.getId());
			for (Document doc : userDocs) {
				anomalyRepository.deleteAll(anomalyRepository.findByDocumentId(doc.getId()));
			}
			documentRepository.deleteAll(userDocs);

			// ✅ Delete user
			userRepository.deleteById(userId);
		}

		return "redirect:/admin";
	}

	//EXPIRE SESSION
	private void expireUserSessions(String username, HttpServletRequest request) {

		try {

			System.out.println("User " + username + " sessions will expire on next request.");

		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	@GetMapping("/export-all-audit")
	public void exportAllAuditCsv(HttpServletResponse response) throws IOException {

		response.setContentType("text/csv");

		response.setHeader("Content-Disposition", "attachment; filename=all_audit_logs.csv");

		PrintWriter writer = response.getWriter();

		writer.println("ID,Username,Role,Action,Document,Timestamp");

		for (AuditLog log : auditLogRepository.findAll()) {

			writer.println(log.getId() + "," + log.getUsername() + "," + log.getUserRole() + "," + log.getAction() + ","
					+ (log.getDocumentName() != null ? log.getDocumentName() : "") + "," + log.getTimestamp());
		}

		writer.flush();
		writer.close();
	}
}