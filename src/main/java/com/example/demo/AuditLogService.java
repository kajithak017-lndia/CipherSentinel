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

			AuditLog log = new AuditLog();
			log.setUserId(user != null ? user.getId() : 0);
			log.setUsername(username);
			log.setUserRole(user != null ? user.getRole() : "USER");
			log.setAction(action);
			log.setDetails(details);
			log.setDocumentName(documentName);
			log.setDocumentType(documentType);
			log.setIpAddress(request != null ? request.getRemoteAddr() : "localhost");

			auditLogRepository.save(log);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void log(String username, String action, String details, Document doc, HttpServletRequest request) {

		try {

			User user = userRepository.findByUsername(username);

			AuditLog log = new AuditLog();

			log.setUserId(user != null ? user.getId() : 0);

			log.setUsername(username);

			log.setUserRole(user != null ? user.getRole() : "USER");

			log.setAction(action);

			log.setDetails(details);

			if (doc != null) {

				log.setDocumentId(doc.getId());

				log.setDocumentName(doc.getFileName());

				log.setDocumentType(doc.getDocumentType());
			}

			log.setIpAddress(request != null ? request.getRemoteAddr() : "localhost");

			auditLogRepository.save(log);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void log(String username, String action, String details, HttpServletRequest request) {

		log(username, action, details, null, request);
	}
}
