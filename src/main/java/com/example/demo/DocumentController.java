package com.example.demo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;

import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Controller
public class DocumentController {

	@Autowired
	private DocumentService documentService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AnomalyRepository anomalyRepository;

	@Autowired
	private AuditLogRepository auditLogRepository;

	@Autowired
	private DocumentRepository documentRepository;

	@Autowired
	private AnomalyDetectionService anomalyDetectionService;

	@Autowired
	private AuditLogService auditLogService;
	
	@Autowired
	private EmailService emailService;
	
	@Autowired
	private LoanApplicationRepository loanApplicationRepository;
	
	@GetMapping("/upload")
	public String uploadPage(@RequestParam(required = false) Integer applicationId, Model model) {
	    populateUploadModel(applicationId, model);
	    return "upload";
	}

	@PostMapping("/upload")
	public String uploadDocument(
	        @RequestParam("file") MultipartFile file,
	        @RequestParam("documentType") String documentType,
	        @RequestParam(value = "applicationId", required = false) Integer applicationId,
	        Authentication auth,
	        Model model,HttpServletRequest request) {

	    if (file == null || file.isEmpty()) {
	        model.addAttribute("error", "Please select a document.");
	        populateUploadModel(applicationId, model);
	        return "upload";
	    }

	    try {
	        User user = userRepository.findByUsername(auth.getName());
	        documentService.saveDocument(file, user.getId(), documentType, applicationId,
	                auth.getName(), request);   // <-- was missing these two args
	        model.addAttribute("success", "Document uploaded successfully.");
	    } catch (Exception e) {
	        model.addAttribute("error", "Upload failed : " + e.getMessage());
	    }

	    populateUploadModel(applicationId, model);
	    return "upload";
	}

	/** Shared model setup so GET and POST always render identical, complete state. */
	private void populateUploadModel(Integer applicationId, Model model) {

	    if (applicationId == null) {
	        return;
	    }

	    LoanApplication app = loanApplicationRepository.findById(applicationId).orElse(null);
	    model.addAttribute("loanApplication", app);
	    if (app != null && app.getService() != null) {

	        List<String> required = app.getService().getRequiredDocumentsList();
	        List<Document> uploaded = documentRepository.findByApplicationId(applicationId);

	        List<String> uploadedTypes = uploaded.stream()
	                .map(Document::getDocumentType)
	                .collect(java.util.stream.Collectors.toList());

	        List<String> missing = required.stream()
	                .filter(r -> !uploadedTypes.contains(r))
	                .collect(java.util.stream.Collectors.toList());

	        model.addAttribute("requiredDocuments", required);
	        model.addAttribute("uploadedDocuments", uploaded);
	        model.addAttribute("missingDocuments", missing);
	    }
	}

	/**
	 * Recalculates and saves the stored progress fields (uploadedDocuments, totalDocuments,
	 * completedPercentage, status) on a LoanApplication based on what is ACTUALLY in the
	 * database right now. Must be called any time a document tied to an application is
	 * added or removed outside the normal saveDocument() flow — e.g. after a delete —
	 * otherwise the upload page's progress bar/label goes stale (shows old counts forever).
	 */
	private void recalculateApplicationProgress(Integer applicationId) {

	    if (applicationId == null) {
	        return;
	    }

	    LoanApplication app = loanApplicationRepository.findById(applicationId).orElse(null);
	    if (app == null || app.getService() == null) {
	        return;
	    }

	    List<String> required = app.getService().getRequiredDocumentsList();
	    List<Document> remaining = documentRepository.findByApplicationId(applicationId);

	    List<String> uploadedTypes = remaining.stream()
	            .map(Document::getDocumentType)
	            .distinct()
	            .collect(java.util.stream.Collectors.toList());

	    long uploadedCount = required.stream()
	            .filter(uploadedTypes::contains)
	            .count();

	    int totalRequired = required.size();
	    int pct = totalRequired > 0 ? (int) (uploadedCount * 100 / totalRequired) : 0;

	    app.setTotalDocuments(totalRequired);
	    app.setUploadedDocuments((int) uploadedCount);
	    app.setCompletedPercentage(pct);

	    // Only touch status if the application hasn't already moved past the applicant's
	    // own editing stage — never silently revert a status once it's under review or decided.
	    String currentStatus = app.getStatus();
	    boolean isEditableStage = currentStatus == null
	            || "SUBMITTED".equals(currentStatus)
	            || "Pending Verification".equals(currentStatus)
	            || "DOCUMENTS_COMPLETE".equals(currentStatus);

	    if (isEditableStage) {
	        if (uploadedCount == 0) {
	            app.setStatus("SUBMITTED");
	        } else if (uploadedCount < totalRequired) {
	            app.setStatus("Pending Verification");
	        } else {
	            app.setStatus("DOCUMENTS_COMPLETE");
	        }
	    }

	    loanApplicationRepository.save(app);
	}

	@GetMapping("/documents")
	public String viewDocuments(Authentication auth, Model model) {
	    User user = userRepository.findByUsername(auth.getName());
	    model.addAttribute("applicationsWithDocuments", documentService.getApplicationsWithDocumentsForUser(user.getId()));
	    model.addAttribute("uncategorizedDocuments", documentService.getUncategorizedDocuments(user.getId()));
	    return "documents";
	}
	@PostMapping("/submit-application/{id}")
	public String submitApplication(@PathVariable(required = false) Integer id, Authentication auth) {

	    if (id == null) {
	        return "redirect:/documents";
	    }

	    LoanApplication app = loanApplicationRepository.findById(id).orElse(null);
	    if (app == null || app.getService() == null) {
	        return "redirect:/documents";
	    }

	    int required = app.getService().getRequiredDocumentsList().size();
	    int uploaded = app.getUploadedDocuments() != null ? app.getUploadedDocuments() : 0;

	    if (uploaded >= required) {
	        app.setStatus("UNDER_REVIEW");
	        loanApplicationRepository.save(app);
	    }

	    return "redirect:/documents";
	}
	@PostMapping("/cancel-application/{id}")
	public String cancelApplication(@PathVariable(required = false) Integer id, Authentication auth) {

	    if (id == null) {
	        return "redirect:/documents";
	    }

	    LoanApplication app = loanApplicationRepository.findById(id).orElse(null);
	    if (app == null) {
	        return "redirect:/documents";
	    }

	    User user = userRepository.findByUsername(auth.getName());

	    // Only the owner can cancel their own application
	    if (app.getCustomer() == null || app.getCustomer().getId() != user.getId()) {
	        return "redirect:/documents";
	    }

	    // Safety guard: only cancellable while still editable, not once it's under review or decided
	    if ("UNDER_REVIEW".equals(app.getStatus())
	            || "MANAGER_REVIEW".equals(app.getStatus())
	            || "APPROVED".equals(app.getStatus())
	            || "REJECTED_BY_OFFICER".equals(app.getStatus())
	            || "REJECTED_BY_MANAGER".equals(app.getStatus())) {
	        return "redirect:/documents";
	    }

	    List<Document> docs = documentRepository.findByApplicationId(id);

	    for (Document doc : docs) {
	        List<Anomaly> anomalies = anomalyRepository.findByDocumentId(doc.getId());
	        anomalyRepository.deleteAll(anomalies);

	        List<AuditLog> logs = auditLogRepository.findByDocumentId(doc.getId());
	        auditLogRepository.deleteAll(logs);

	        try {
	            Path filePath = Paths.get("uploads/" + doc.getFileName());
	            Files.deleteIfExists(filePath);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }

	    documentRepository.deleteAll(docs);
	    loanApplicationRepository.delete(app);

	    return "redirect:/documents";
	}
	@GetMapping("/anomalies")
	public String viewAnomalies(Authentication auth, Model model) {

		User user = userRepository.findByUsername(auth.getName());

		List<Document> userDocs = documentRepository.findByUploadedBy(user.getId());

		List<Anomaly> userAnomalies = new ArrayList<>();

		for (Document doc : userDocs) {

			List<Anomaly> anomalies = anomalyRepository.findByDocumentId(doc.getId());

			for (Anomaly anomaly : anomalies) {

				anomaly.setDocumentName(doc.getFileName());
			}

			userAnomalies.addAll(anomalies);
		}

		long safeCount = userDocs.stream().filter(d -> "SAFE".equals(d.getStatus())).count();

		long unsafeCount = userDocs.stream().filter(d -> "UNSAFE".equals(d.getStatus())).count();

		long highCount = userAnomalies.stream().filter(a -> "HIGH".equalsIgnoreCase(a.getSeverity())).count();

		long mediumCount = userAnomalies.stream().filter(a -> "MEDIUM".equalsIgnoreCase(a.getSeverity())).count();

		long lowCount = userAnomalies.stream().filter(a -> "LOW".equalsIgnoreCase(a.getSeverity())).count();
		long metadataCount = userAnomalies.stream()
				.filter(a -> "Metadata Tampering".equalsIgnoreCase(a.getAnomalyType())).count();

		long modificationCount = userAnomalies.stream()
				.filter(a -> "Suspicious Modification".equalsIgnoreCase(a.getAnomalyType())).count();

		long hashCount = userAnomalies.stream().filter(a -> "Hash Mismatch".equalsIgnoreCase(a.getAnomalyType()))
				.count();

		long signatureCount = userAnomalies.stream()
				.filter(a -> "Missing Signature".equalsIgnoreCase(a.getAnomalyType())).count();

		long formattingCount = userAnomalies.stream()
				.filter(a -> "Formatting Anomaly".equalsIgnoreCase(a.getAnomalyType())).count();

		int safetyLevel = 100;

		if (userDocs.size() > 0) {

			safetyLevel = (int) ((safeCount * 100) / userDocs.size());
		}

		model.addAttribute("anomalies", userAnomalies);

		model.addAttribute("totalDocs", userDocs.size());

		model.addAttribute("safeCount", safeCount);

		model.addAttribute("unsafeCount", unsafeCount);

		model.addAttribute("totalIssues", userAnomalies.size());

		model.addAttribute("highCount", highCount);

		model.addAttribute("mediumCount", mediumCount);

		model.addAttribute("lowCount", lowCount);
		model.addAttribute("metadataCount", metadataCount);

		model.addAttribute("modificationCount", modificationCount);

		model.addAttribute("hashCount", hashCount);

		model.addAttribute("signatureCount", signatureCount);

		model.addAttribute("formattingCount", formattingCount);

		model.addAttribute("safetyLevel", safetyLevel);

		return "anomalies";
	}

	// ===== VIEW DOCUMENT =====
	@GetMapping("/view/{id}")
	public String viewDocument(@PathVariable int id, Authentication auth, Model model, HttpServletRequest request) {
		Document doc = documentRepository.findById(id).orElse(null);

		if (doc == null)
			return "redirect:/documents";

		List<Anomaly> issues = anomalyRepository.findByDocumentId(id);

		model.addAttribute("trustScore", doc.getTrustScore());

		model.addAttribute("document", doc);

		model.addAttribute("issues", issues);

		boolean isImage = doc.getFileType() != null && doc.getFileType().toLowerCase().startsWith("image");

		model.addAttribute("isImage", isImage);

		// ✅ LOG DOCUMENT VIEWED
		auditLogService.log(auth.getName(), "DOCUMENT_VIEWED", "Viewed document: " + doc.getFileName(), doc, request);

		return "view_document";
	}

	// ===== VIEW ISSUES =====
	@GetMapping("/issues/{id}")
	public String viewIssues(@PathVariable int id, Authentication auth, Model model, HttpServletRequest request) {

		Document doc = documentRepository.findById(id).orElse(null);

		List<Anomaly> issues = anomalyRepository.findByDocumentId(id);

		model.addAttribute("document", doc);
		model.addAttribute("issues", issues);

		// ✅ LOG ISSUE VIEWED
		if (doc != null) {
			auditLogService.log(auth.getName(), "ISSUES_VIEWED",
					"Viewed issues for: " + doc.getFileName() + " | Issues found: " + issues.size(), doc, request);
		}

		return "issues";
	}

	@PostMapping("/profile/upload-image")
	public String uploadProfileImage(@RequestParam("image") MultipartFile image, Authentication auth) {

		try {
			User user = userRepository.findByUsername(auth.getName());
			if (image.isEmpty()) {
				return "redirect:/profile";
			}

			// Delete old profile image first
			if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {

				Path oldPath = Paths.get("profile-images/" + user.getProfileImage());

				Files.deleteIfExists(oldPath);
			}

			// Save new image
			String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();

			Path path = Paths.get("profile-images/" + fileName);

			Files.write(path, image.getBytes());

			user.setProfileImage(fileName);

			userRepository.save(user);

			// ✅ Log profile change
			saveAuditLog(user, "PROFILE_PHOTO_UPDATED", null, "Profile photo updated");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return "redirect:/profile";
	}

	@GetMapping("/rescan/{id}")
	public String rescanDocument(@PathVariable int id, Authentication auth) {

		try {

			Document doc = documentRepository.findById(id).orElse(null);

			if (doc == null) {
				return "redirect:/documents";
			}

			User user = userRepository.findByUsername(auth.getName());

			if (user == null || doc.getUploadedBy() != user.getId()) {
				return "redirect:/documents";
			}

			Path filePath = Paths.get("uploads/" + doc.getFileName());

			byte[] fileContent = Files.readAllBytes(filePath);

			// SHA-256 verification
			String currentHash = generateHash(fileContent);

			if (!currentHash.equals(doc.getDocumentHash())) {

			    List<Anomaly> oldIssues =
			        anomalyRepository.findByDocumentId(
			            doc.getId());

			    anomalyRepository.deleteAll(
			        oldIssues);

			    Anomaly anomaly = new Anomaly();

			    anomaly.setDocumentId(doc.getId());
			    anomaly.setAnomalyType("TAMPERING_DETECTED");
			    anomaly.setSeverity("HIGH");

			    anomaly.setDescription(
			        "Document content has changed since upload. SHA-256 hash mismatch detected.");

			    anomalyRepository.save(anomaly);

			    doc.setStatus("UNSAFE");

			    // EMAIL ALERT
			    User owner =
			        userRepository.findById(
			            doc.getUploadedBy())
			        .orElse(null);

			    if (owner != null &&
			        owner.getEmail() != null &&
			        !owner.getEmail().isEmpty()) {

			        emailService.sendTamperingAlert(
			            owner.getEmail(),
			            doc.getFileName()
			        );
			    }

			} else {
				List<Anomaly> oldAnomalies = anomalyRepository.findByDocumentId(id);

				anomalyRepository.deleteAll(oldAnomalies);

				doc.setStatus("PENDING");
				documentRepository.save(doc);

				anomalyDetectionService.scanDocument(doc, fileContent, filePath.toString());
			}

			documentRepository.save(doc);

			saveAuditLog(user, "DOCUMENT_RESCANNED", doc, "Document rescanned successfully");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return "redirect:/documents";
	}

	@GetMapping("/delete/{id}")
	public String deleteDocument(@PathVariable int id, Authentication auth) {

		try {

			Document doc = documentRepository.findById(id).orElse(null);

			if (doc != null) {

				User user = userRepository.findByUsername(auth.getName());

				if (user == null || doc.getUploadedBy() != user.getId()) {
					return "redirect:/documents";
				}

				// Capture the parent application's id BEFORE deleting the document,
				// since we need it afterward to recalculate progress.
				Integer applicationId = doc.getApplication() != null ? doc.getApplication().getId() : null;

				List<Anomaly> anomalies = anomalyRepository.findByDocumentId(id);

				anomalyRepository.deleteAll(anomalies);

				Path filePath = Paths.get("uploads/" + doc.getFileName());

				Files.deleteIfExists(filePath);

				documentRepository.deleteById(id);

				// ✅ FIX: keep the parent application's stored progress in sync with reality.
				// Without this, the upload page's progress bar/label and status badge stay
				// frozen at whatever they were before the delete (e.g. still shows
				// "5 of 5 uploaded — DOCUMENTS_COMPLETE" after you've deleted one).
				recalculateApplicationProgress(applicationId);

				saveAuditLog(user, "DOCUMENT_DELETED", doc, "Document deleted successfully");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return "redirect:/documents";
	}

	private void saveAuditLog(User user, String action, Document doc, String details) {

		AuditLog log = new AuditLog();

		log.setUserId(user.getId());
		log.setUsername(user.getUsername());
		log.setUserRole(user.getRole());

		log.setAction(action);

		if (doc != null) {
			log.setDocumentName(doc.getFileName());
			log.setDocumentType(doc.getDocumentType());
			log.setDocumentId(doc.getId());
		}

		log.setDetails(details);

		auditLogRepository.save(log);
	}
	private String generateHash(byte[] content) {

		try {

			java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");

			byte[] hash = md.digest(content);

			StringBuilder sb = new StringBuilder();

			for (byte b : hash) {

				sb.append(String.format("%02x", b));
			}

			return sb.toString();

		} catch (Exception e) {

			return "";
		}
	}

	@GetMapping("/preview/{id}")
	public ResponseEntity<byte[]> previewDocument(@PathVariable int id) {

		try {
			Document doc = documentRepository.findById(id).orElse(null);

			if (doc == null) {
				return ResponseEntity.notFound().build();
			}

			java.nio.file.Path filePath = java.nio.file.Paths.get("uploads/" + doc.getFileName());

			byte[] fileBytes = java.nio.file.Files.readAllBytes(filePath);

			String contentType = "application/octet-stream";
			if (doc.getFileType() != null) {
				contentType = doc.getFileType();
			}

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType(contentType));
			headers.setContentDisposition(ContentDisposition.builder("inline").filename(doc.getFileName()).build());

			return ResponseEntity.ok().headers(headers).body(fileBytes);

		} catch (Exception e) {
			return ResponseEntity.internalServerError().build();
		}
	}
	@GetMapping("/edit-document/{id}")
	public String editDocument(
	        @PathVariable int id,
	        Model model) {

	    try {

	        Document doc =
	            documentRepository.findById(id)
	            .orElse(null);

	        if(doc == null) {
	            return "redirect:/documents";
	        }

	        Path filePath =
	            Paths.get(
	                "uploads/" +
	                doc.getFileName());

	        String content =
	            Files.readString(filePath);

	        model.addAttribute(
	            "document",
	            doc);

	        model.addAttribute(
	            "content",
	            content);

	        return "edit_document";

	    } catch(Exception e) {

	        e.printStackTrace();

	        return "redirect:/documents";
	    }
	}
	@PostMapping("/edit-document/{id}")
	public String saveEditedDocument(
	        @PathVariable int id,
	        @RequestParam String content)
	        throws Exception {

	    Document doc =
	        documentRepository.findById(id)
	        .orElse(null);

	    if(doc == null) {
	        return "redirect:/documents";
	    }

	    Path filePath =
	        Paths.get(
	            "uploads/" +
	            doc.getFileName());

	    Files.write(
	        filePath,
	        content.getBytes());

	    return "redirect:/view/" + id;
	}
	@GetMapping("/download/{id}")
	public ResponseEntity<byte[]> downloadDocument(@PathVariable int id) {

	    try {
	        Document doc = documentRepository.findById(id).orElse(null);
	        if (doc == null) {
	            return ResponseEntity.notFound().build();
	        }

	        Path filePath = Paths.get("uploads/" + doc.getFileName());
	        byte[] fileBytes = Files.readAllBytes(filePath);

	        String contentType = doc.getFileType() != null ? doc.getFileType() : "application/octet-stream";

	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.parseMediaType(contentType));
	        headers.setContentDisposition(ContentDisposition.builder("attachment").filename(doc.getFileName()).build());

	        return ResponseEntity.ok().headers(headers).body(fileBytes);

	    } catch (Exception e) {
	        return ResponseEntity.internalServerError().build();
	    }
	}
	@GetMapping("/export-audit")
	public void exportAuditCsv(Authentication auth, HttpServletResponse response) throws IOException {

		response.setContentType("text/csv");

		response.setHeader("Content-Disposition", "attachment; filename=my_audit_logs.csv");

		PrintWriter writer = response.getWriter();

		writer.println("ID,Username,Role,Action,Document,Timestamp");

		List<AuditLog> logs = auditLogRepository.findByUsernameOrderByTimestampDesc(auth.getName());

		for (AuditLog log : logs) {

			writer.println(log.getId() + "," + log.getUsername() + "," + log.getUserRole() + "," + log.getAction() + ","
					+ (log.getDocumentName() != null ? log.getDocumentName() : "") + "," + log.getTimestamp());
		}

		writer.flush();
		writer.close();
	}
}