package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendUnsafeDocumentAlert(String toEmail, String documentName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("CipherSentinel Alert");
            message.setText("Document " + documentName + " has been marked UNSAFE.");
            mailSender.send(message);
        } catch (Exception e) {
            System.out.println("EMAIL FAILED: " + e.getMessage());
        }
    }

    public void sendTamperingAlert(String toEmail, String documentName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("🚨 CipherSentinel - Document Tampering Detected");
            message.setText(
                "Dear User,\n\n" +
                "CipherSentinel has detected a SHA-256 integrity violation.\n\n" +
                "Document: " + documentName + "\n" +
                "Status: UNSAFE\n\n" +
                "The uploaded document appears to have been modified after upload.\n\n" +
                "Please review the document immediately.\n\n" +
                "Regards,\n" +
                "CipherSentinel Security Engine");
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendApplicationApproved(String toEmail, String applicationNumber, String serviceName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("✅ CipherSentinel - Application Approved");
            message.setText(
                "Dear Customer,\n\n" +
                "Your application " + applicationNumber + " for " + serviceName + " has been APPROVED.\n\n" +
                "Our team will contact you shortly with the next steps.\n\n" +
                "Regards,\n" +
                "CipherSentinel Banking Team");
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendApplicationRejected(String toEmail, String applicationNumber, String serviceName, String reason) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("❌ CipherSentinel - Application Update");
            message.setText(
                "Dear Customer,\n\n" +
                "Your application " + applicationNumber + " for " + serviceName + " has been rejected.\n\n" +
                "Reason: " + (reason != null && !reason.isBlank() ? reason : "Not specified") + "\n\n" +
                "If you have questions, please contact your branch.\n\n" +
                "Regards,\n" +
                "CipherSentinel Banking Team");
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}