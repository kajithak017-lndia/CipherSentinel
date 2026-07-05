package com.example.demo;

import jakarta.persistence.*;
@Entity
@Table(name = "bank_service")
public class BankService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String serviceName;

    @Column(length = 500)
    private String description;

    // Comma-separated list, e.g. "PAN Card,Aadhaar,Salary Slip,Bank Statement,Land Record"
    @Column(length = 500)
    private String requiredDocuments;

    private Boolean active = true;

    public BankService() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequiredDocuments() {
        return requiredDocuments;
    }

    public void setRequiredDocuments(String requiredDocuments) {
        this.requiredDocuments = requiredDocuments;
    }

    // Helper for Thymeleaf: splits the comma string into a List
    @Transient
    public java.util.List<String> getRequiredDocumentsList() {
        if (requiredDocuments == null || requiredDocuments.isBlank()) {
            return java.util.List.of();
        }
        return java.util.Arrays.stream(requiredDocuments.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
    @Transient
    public String getIcon() {
        if (serviceName == null) return "📁";
        switch (serviceName.trim().toLowerCase()) {
            case "home loan": return "🏠";
            case "personal loan": return "💳";
            case "vehicle loan": return "🚗";
            case "education loan": return "🎓";
            case "gold loan": return "🪙";
            case "account opening": return "📂";
            case "kyc update": return "🪪";
            default: return "📁";
        }
    }
}