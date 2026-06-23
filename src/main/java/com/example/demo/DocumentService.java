package com.example.demo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private AnomalyDetectionService anomalyDetectionService;

    @Autowired
    private AuditLogRepository auditLogRepository;
   
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmailService emailService;

    private String uploadDir = "uploads/";

    public Document saveDocument(
        MultipartFile file, 
        int userId,
        String documentType) throws IOException {
    	if(file == null ||
    			   file.isEmpty()) {

    			    throw new RuntimeException(
    			        "Please select a document.");
    			}
    	String fileName =
    		    System.currentTimeMillis() +
    		    "_" +
    		    file.getOriginalFilename();

    		Path filePath =
    		    Paths.get(uploadDir, fileName);

    		Files.createDirectories(filePath.getParent());

    		Files.write(filePath, file.getBytes());
    		System.out.println("Saved File = " + filePath.toAbsolutePath());

    		System.out.println(
    		    "File Exists = " +
    		    Files.exists(filePath)
    		);

    		System.out.println(
    		    "File Size = " +
    		    Files.size(filePath)
    		);
        // Save document to DB
        Document doc = new Document();
        doc.setFileName(fileName);
        doc.setFileType(file.getContentType());
        System.out.println(
        	    "Uploaded Content Type = "
        	    + file.getContentType());
        doc.setUploadedBy(userId);
        doc.setDocumentType(documentType);
        doc.setDocumentHash(
        	    generateHash(file.getBytes())
        	);
        Document savedDoc = documentRepository.save(doc);

        // Scan for anomalies!
        
        anomalyDetectionService
        .scanDocument(
            savedDoc,
            file.getBytes(),
            filePath.toString()
        );
        documentRepository.save(savedDoc);
        if ("UNSAFE".equals(
                savedDoc.getStatus())) {

            User user =
                userRepository.findById(userId)
                    .orElse(null);

            if (user != null &&
                user.getEmail() != null &&
                !user.getEmail().isEmpty()) {

                emailService
                    .sendUnsafeDocumentAlert(
                        user.getEmail(),
                        savedDoc.getFileName());
            }
        }

     // In saveDocument method
        AuditLog log = new AuditLog();
        log.setUserId(userId);

        // Get username properly
        User u = userRepository.findById(userId)
                .orElse(null);
        if (u != null) {
            log.setUsername(u.getUsername());
            log.setUserRole(u.getRole());
        } else {
            log.setUsername("Unknown");
            log.setUserRole("USER");
        }
        log.setAction("DOCUMENT_UPLOADED");
        log.setDocumentName(fileName);
        log.setDocumentType(documentType);
       
        log.setDetails(
            "Uploaded: " + fileName +
            " | Type: " + documentType +
            " | Status: " + savedDoc.getStatus());
        auditLogRepository.save(log);
        return savedDoc;
    }
    private String generateHash(byte[] content) {

        try {

            MessageDigest md =
                    MessageDigest.getInstance("SHA-256");

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
}
