package com.example.demo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private LoanApplicationRepository loanApplicationRepository;

    @Autowired
    private AnomalyDetectionService anomalyDetectionService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    private String uploadDir = "uploads/";

    public Document saveDocument(MultipartFile file, int userId, String documentType, Integer applicationId,
                                 String username, HttpServletRequest request) throws IOException {

        auditLogService.log(username, "DOCUMENT_UPLOAD_STARTED",
                "Started uploading type: " + documentType + " | Application: " + applicationId, request);

        if (file == null || file.isEmpty()) {
            auditLogService.log(username, "DOCUMENT_UPLOAD_FAILED",
                    "No file selected for type: " + documentType, request);
            throw new RuntimeException("Please select a document.");
        }

        try {
            if (applicationId != null) {
                LoanApplication app = loanApplicationRepository.findById(applicationId).orElse(null);

                if (app != null && app.getService() != null) {
                    int requiredCount = app.getService().getRequiredDocumentsList().size();
                    long uploadedCount = documentRepository.countByApplicationId(applicationId);

                    if (uploadedCount >= requiredCount) {
                        throw new RuntimeException("Only " + requiredCount + " documents are allowed for this application.");
                    }

                    if (documentRepository.existsByApplicationIdAndDocumentTypeIgnoreCase(applicationId, documentType)) {
                        throw new RuntimeException("This document type has already been uploaded.");
                    }
                }
            }

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir, fileName);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, file.getBytes());

            Document doc = new Document();
            doc.setFileName(fileName);
            doc.setFileType(file.getContentType());
            doc.setUploadedBy(userId);
            doc.setDocumentType(documentType);

            String hash = generateHash(file.getBytes());
            doc.setDocumentHash(hash);

            if (applicationId != null) {
                LoanApplication app = loanApplicationRepository.findById(applicationId).orElse(null);
                doc.setApplication(app);
                if (app != null && app.getService() != null) {
                    doc.setServiceType(app.getService().getServiceName());
                }
            }

            Document savedDoc = documentRepository.save(doc);

            auditLogService.log(username, "SHA256_HASH_GENERATED",
                    "Hash generated for " + fileName + ": " + hash, savedDoc, request);

            auditLogService.log(username, "AI_SCAN_STARTED",
                    "Anomaly/AI scan started for " + fileName, savedDoc, request);

            anomalyDetectionService.scanDocument(savedDoc, file.getBytes(), filePath.toString());
            documentRepository.save(savedDoc);

            auditLogService.log(username, "AI_SCAN_COMPLETED",
                    "AI scan completed | Status: " + savedDoc.getStatus()
                            + " | Trust Score: " + savedDoc.getTrustScore(), savedDoc, request);

            if ("UNSAFE".equalsIgnoreCase(savedDoc.getStatus())
                    || "SUSPICIOUS".equalsIgnoreCase(savedDoc.getStatus())) {
                auditLogService.log(username, "ANOMALY_DETECTED",
                        "Anomaly detected on " + fileName + " | Status: " + savedDoc.getStatus(), savedDoc, request);
            }

            if (applicationId != null) {
                updateApplicationProgress(applicationId, username, request);
            }

            if ("UNSAFE".equals(savedDoc.getStatus())) {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
                    emailService.sendUnsafeDocumentAlert(user.getEmail(), savedDoc.getFileName());
                    auditLogService.log(username, "EMAIL_ALERT_SENT",
                            "Unsafe document alert sent to " + user.getEmail(), savedDoc, request);
                }
            }

            auditLogService.log(username, "DOCUMENT_UPLOADED",
                    "Uploaded: " + fileName + " | Type: " + documentType
                            + " | Application: " + applicationId
                            + " | Status: " + savedDoc.getStatus(), savedDoc, request);

            return savedDoc;

        } catch (Exception e) {
            auditLogService.log(username, "DOCUMENT_UPLOAD_FAILED",
                    "Upload failed for type " + documentType + ": " + e.getMessage(), request);
            throw e;
        }
    }

    public void updateApplicationProgress(int applicationId, String username, HttpServletRequest request) {
        LoanApplication app = loanApplicationRepository.findById(applicationId).orElse(null);
        if (app == null || app.getService() == null) return;

        List<String> required = app.getService().getRequiredDocumentsList();
        List<Document> appDocs = documentRepository.findByApplicationId(applicationId);
        List<String> uploadedTypes = appDocs.stream()
                .map(Document::getDocumentType)
                .collect(Collectors.toList());

        long uploadedCount = required.stream().filter(uploadedTypes::contains).count();

        app.setTotalDocuments(required.size());
        app.setUploadedDocuments((int) uploadedCount);
        app.setCompletedPercentage(required.isEmpty() ? 100 : (int) ((uploadedCount * 100.0) / required.size()));

        String previousStatus = app.getStatus();

        if (required.size() > 0 && uploadedCount >= required.size()) {
            app.setStatus("DOCUMENTS_COMPLETE");
        } else if (uploadedCount > 0) {
            app.setStatus("IN_PROGRESS");
        } else {
            app.setStatus("SUBMITTED");
        }

        if (previousStatus == null || !previousStatus.equals(app.getStatus())) {
            auditLogService.log(username, "APPLICATION_STATUS_CHANGED",
                    "Application " + app.getApplicationNumber()
                            + " status: " + previousStatus + " -> " + app.getStatus(), request);

            if ("DOCUMENTS_COMPLETE".equals(app.getStatus())) {
                auditLogService.log(username, "DOCUMENTS_COMPLETE",
                        "All required documents uploaded for " + app.getApplicationNumber(), request);
            }
        }

        loanApplicationRepository.save(app);
    }

    public void updateApplicationProgress(int applicationId) {
        updateApplicationProgress(applicationId, "system", null);
    }

    public List<LoanApplication> getApplicationsForUser(int userId) {
        return loanApplicationRepository.findByCustomerId(userId);
    }

    public List<Document> getUncategorizedDocuments(int userId) {
        return documentRepository.findByUploadedBy(userId).stream()
                .filter(d -> d.getApplication() == null)
                .collect(Collectors.toList());
    }

    private String generateHash(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
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

    public List<Document> getDocumentsByUser(int userId) {
        return documentRepository.findByUploadedBy(userId);
    }

    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    public List<ApplicationWithDocuments> getApplicationsWithDocumentsForUser(int userId) {
        List<LoanApplication> apps = loanApplicationRepository.findByCustomerId(userId);
        List<ApplicationWithDocuments> result = new java.util.ArrayList<>();
        for (LoanApplication app : apps) {
            result.add(new ApplicationWithDocuments(app, documentRepository.findByApplicationId(app.getId())));
        }
        return result;
    }
}