package com.example.demo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int userId;
    private String username;
    private String userRole;
    private String action;
    private String details;
    private String documentName;
    private String documentType;
    private String ipAddress;
    private LocalDateTime timestamp;
    private Integer documentId;

    @PrePersist
    public void prePersist() {
        if (timestamp == null)
            timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String u) { this.username = u; }
    public String getUserRole() { return userRole; }
    public void setUserRole(String r) { this.userRole = r; }
    public String getAction() { return action; }
    public void setAction(String a) { this.action = a; }
    public String getDetails() { return details; }
    public void setDetails(String d) { this.details = d; }
    public String getDocumentName() { return documentName; }
    public void setDocumentName(String n) { this.documentName = n; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String t) { this.documentType = t; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ip) { this.ipAddress = ip; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime t) { this.timestamp = t; }
    public Integer getDocumentId() {
        return documentId;
    }

    public void setDocumentId(
            Integer documentId) {
        this.documentId = documentId;
    }
	
}