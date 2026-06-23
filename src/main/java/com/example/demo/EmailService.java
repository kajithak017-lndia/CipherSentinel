package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendUnsafeDocumentAlert(
            String toEmail,
            String documentName) {

        SimpleMailMessage message =
            new SimpleMailMessage();

        message.setTo(toEmail);

        message.setSubject(
            "🚨 CipherSentinel Alert");

        message.setText(
            "Hello,\n\n" +
            "Your document '" +
            documentName +
            "' has been marked as UNSAFE.\n\n" +
            "Please review the detected issues.\n\n" +
            "CipherSentinel");

        mailSender.send(message);
    }
    public void sendTamperingAlert(
            String toEmail,
            String documentName) {

        try {

            SimpleMailMessage message =
                new SimpleMailMessage();

            message.setTo(toEmail);

            message.setSubject(
                "🚨 CipherSentinel - Document Tampering Detected");

            message.setText(
                "Dear User,\n\n" +

                "CipherSentinel has detected " +
                "a SHA-256 integrity violation.\n\n" +

                "Document: " + documentName + "\n" +

                "Status: UNSAFE\n\n" +

                "The uploaded document appears " +
                "to have been modified after upload.\n\n" +

                "Please review the document immediately.\n\n" +

                "Regards,\n" +
                "CipherSentinel Security Engine");

            mailSender.send(message);

        } catch(Exception e) {

            e.printStackTrace();
        }
    }
}