package com.example.demo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

	@Autowired
	private AuditLogRepository auditLogRepository;

	@Autowired
	private UserRepository userRepository;

	public void log(String username, String action, String details, String documentName, String documentType,
			HttpServletRequest request) {
		try {
			User user = userRepository.findByUsername(username);
			AuditLog log = buildBaseLog(user, username, action, details, request);
			log.setDocumentName(documentName);
			log.setDocumentType(documentType);
			auditLogRepository.save(log);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void log(String username, String action, String details, Document doc, HttpServletRequest request) {
		try {
			User user = userRepository.findByUsername(username);
			AuditLog log = buildBaseLog(user, username, action, details, request);

			if (doc != null) {
				log.setDocumentId(doc.getId());
				log.setDocumentName(doc.getFileName());
				log.setDocumentType(doc.getDocumentType());
				log.setTrustScore(doc.getTrustScore());
				log.setSimilarityScore(doc.getSimilarityScore());
				log.setCurrentStatus(doc.getStatus());

				if (doc.getApplication() != null) {
					LoanApplication app = doc.getApplication();
					log.setApplicationId(app.getId());
					log.setApplicationNumber(app.getApplicationNumber());
					if (app.getService() != null) {
						log.setServiceName(app.getService().getServiceName());
					}
				}
			}

			auditLogRepository.save(log);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void log(String username, String action, String details, HttpServletRequest request) {
		log(username, action, details, (Document) null, request);
	}

	// ===== SHARED BUILDER =====
	private AuditLog buildBaseLog(User user, String username, String action, String details, HttpServletRequest request) {

		AuditLog log = new AuditLog();

		log.setUserId(user != null ? user.getId() : 0);
		log.setUsername(username);
		log.setUserRole(user != null ? user.getRole() : "USER");

		log.setAction(action);
		log.setDetails(details);
		log.setModule(getModule(action));
		log.setSeverity(getSeverity(action));
		log.setActionResult(action != null && action.contains("FAILED") ? "FAILURE" : "SUCCESS");

		if (request != null) {
			log.setIpAddress(request.getRemoteAddr());
			try {
				log.setSessionId(request.getSession().getId());
			} catch (Exception ignored) {}

			String agent = request.getHeader("User-Agent");
			log.setBrowser(getBrowser(agent));
			log.setOperatingSystem(getOS(agent));
		} else {
			log.setIpAddress("localhost");
		}

		return log;
	}

	// ===== MODULE CLASSIFICATION =====
	private String getModule(String action) {
		if (action == null) return "System";

		if (action.contains("LOGIN") || action.contains("LOGOUT") || action.contains("PASSWORD"))
			return "Authentication";
		if (action.contains("APPLICATION") || action.contains("OFFICER") || action.contains("MANAGER"))
			return "Loan Processing";
		if (action.contains("DOCUMENT") || action.contains("HASH") || action.contains("SCAN"))
			return "Document Verification";
		if (action.contains("ANOMALY") || action.contains("TAMPERING"))
			return "Security";
		if (action.contains("PROFILE"))
			return "Profile";
		if (action.contains("AUDIT"))
			return "Audit";
		if (action.contains("ROLE") || action.contains("USER_DELETED") || action.contains("ADMIN"))
			return "Administration";

		return "System";
	}

	// ===== SEVERITY CLASSIFICATION =====
	private String getSeverity(String action) {
		if (action == null) return "INFO";

		if (action.contains("TAMPERING") || action.contains("FRAUD"))
			return "CRITICAL";
		if (action.contains("FAILED") || action.contains("DENIED") || action.contains("MISMATCH")
				|| action.contains("REJECTED") || action.contains("UNSAFE"))
			return "HIGH";
		if (action.contains("ANOMALY") || action.contains("BLOCKED"))
			return "MEDIUM";

		return "INFO";
	}

	private String getBrowser(String ua) {
		if (ua == null) return "Unknown";
		if (ua.contains("Edg")) return "Microsoft Edge";
		if (ua.contains("Chrome")) return "Google Chrome";
		if (ua.contains("Firefox")) return "Mozilla Firefox";
		if (ua.contains("Safari")) return "Safari";
		return "Unknown";
	}

	private String getOS(String ua) {
		if (ua == null) return "Unknown";
		if (ua.contains("Windows")) return "Windows";
		if (ua.contains("Mac")) return "MacOS";
		if (ua.contains("Linux")) return "Linux";
		if (ua.contains("Android")) return "Android";
		return "Unknown";
	}
}