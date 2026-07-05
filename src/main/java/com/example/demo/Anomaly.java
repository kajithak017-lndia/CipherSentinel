package com.example.demo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "anomalies")
public class Anomaly {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	private int documentId;
	private String anomalyType;
	private String severity;
	private LocalDateTime detectedAt;
	private String description;

	@PrePersist
	public void prePersist() {
		detectedAt = LocalDateTime.now();
	}

	@Transient
	private String documentName;

	// ===== NEW: application number, resolved at read-time in the controller =====
	@Transient
	private String applicationNumber;

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getDocumentId() {
		return documentId;
	}
	public void setDocumentId(int documentId) {
		this.documentId = documentId;
	}
	public String getAnomalyType() {
		return anomalyType;
	}
	public void setAnomalyType(String anomalyType) {
		this.anomalyType = anomalyType;
	}
	public String getSeverity() {
		return severity;
	}
	public void setSeverity(String severity) {
		this.severity = severity;
	}
	public LocalDateTime getDetectedAt() {
		return detectedAt;
	}
	public void setDetectedAt(LocalDateTime detectedAt) {
		this.detectedAt = detectedAt;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getDocumentName() {
		return documentName;
	}
	public void setDocumentName(String documentName) {
		this.documentName = documentName;
	}

	public String getApplicationNumber() {
		return applicationNumber;
	}
	public void setApplicationNumber(String applicationNumber) {
		this.applicationNumber = applicationNumber;
	}
}