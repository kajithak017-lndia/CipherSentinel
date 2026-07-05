package com.example.demo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_application")
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true)
    private String applicationNumber;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private User customer;

    @ManyToOne
    @JoinColumn(name = "manager_id")
    private User manager;

    @ManyToOne
    @JoinColumn(name = "officer_id")
    private User officer;

    @ManyToOne
    @JoinColumn(name = "service_id")
    private BankService service;

    private String status;
    private Double loanAmount;
    private Integer trustScore;
    private Integer overallTrustScore;
    private String riskLevel;
    private String remarks;
    private Integer totalDocuments;
    private Integer uploadedDocuments;
    private Integer completedPercentage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ==========================
    // OFFICER REVIEW
    // ==========================
    @Column(length = 1000)
    private String officerRemarks;

    private LocalDateTime officerDecisionAt;

    // ==========================
    // MANAGER REVIEW (FINAL DECISION)
    // ==========================
    @Column(length = 1000)
    private String managerRemarks;

    private LocalDateTime managerDecisionAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "SUBMITTED";
        if (totalDocuments == null) {
            totalDocuments = (service != null) ? service.getRequiredDocumentsList().size() : 0;
        }
        if (uploadedDocuments == null) uploadedDocuments = 0;
        if (completedPercentage == null) completedPercentage = 0;
        if (applicationNumber == null) {
            applicationNumber = "APP" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getApplicationNumber() { return applicationNumber; }
    public void setApplicationNumber(String applicationNumber) { this.applicationNumber = applicationNumber; }

    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }

    public User getManager() { return manager; }
    public void setManager(User manager) { this.manager = manager; }

    public User getOfficer() { return officer; }
    public void setOfficer(User officer) { this.officer = officer; }

    public BankService getService() { return service; }
    public void setService(BankService service) { this.service = service; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getLoanAmount() { return loanAmount; }
    public void setLoanAmount(Double loanAmount) { this.loanAmount = loanAmount; }

    public Integer getTrustScore() { return trustScore; }
    public void setTrustScore(Integer trustScore) { this.trustScore = trustScore; }

    public Integer getOverallTrustScore() { return overallTrustScore; }
    public void setOverallTrustScore(Integer overallTrustScore) { this.overallTrustScore = overallTrustScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public Integer getTotalDocuments() { return totalDocuments; }
    public void setTotalDocuments(Integer totalDocuments) { this.totalDocuments = totalDocuments; }

    public Integer getUploadedDocuments() { return uploadedDocuments; }
    public void setUploadedDocuments(Integer uploadedDocuments) { this.uploadedDocuments = uploadedDocuments; }

    public Integer getCompletedPercentage() { return completedPercentage; }
    public void setCompletedPercentage(Integer completedPercentage) { this.completedPercentage = completedPercentage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getOfficerRemarks() { return officerRemarks; }
    public void setOfficerRemarks(String officerRemarks) { this.officerRemarks = officerRemarks; }

    public LocalDateTime getOfficerDecisionAt() { return officerDecisionAt; }
    public void setOfficerDecisionAt(LocalDateTime officerDecisionAt) { this.officerDecisionAt = officerDecisionAt; }

    public String getManagerRemarks() { return managerRemarks; }
    public void setManagerRemarks(String managerRemarks) { this.managerRemarks = managerRemarks; }

    public LocalDateTime getManagerDecisionAt() { return managerDecisionAt; }
    public void setManagerDecisionAt(LocalDateTime managerDecisionAt) { this.managerDecisionAt = managerDecisionAt; }
}