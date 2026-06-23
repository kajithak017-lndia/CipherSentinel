package com.example.demo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	private String fileName;

	private String fileType;

	private String documentType;

	private int uploadedBy;

	private String status;

	private String documentHash;

	private LocalDateTime uploadTime;
	private Integer trustScore;

	private Integer similarityScore;

	@PrePersist
	public void prePersist() {

	    if(uploadTime == null) {
	        uploadTime = LocalDateTime.now();
	    }

	    if(status == null) {
	        status = "PENDING";
	    }
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFileType() {
		return fileType;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	public String getDocumentType() {
		return documentType;
	}

	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}

	public int getUploadedBy() {
		return uploadedBy;
	}

	public void setUploadedBy(int uploadedBy) {
		this.uploadedBy = uploadedBy;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getDocumentHash() {
		return documentHash;
	}

	public void setDocumentHash(String documentHash) {
		this.documentHash = documentHash;
	}

	public LocalDateTime getUploadTime() {
		return uploadTime;
	}

	public void setUploadTime(LocalDateTime uploadTime) {
		this.uploadTime = uploadTime;
	}

	public Integer getTrustScore() {
		return trustScore;
	}

	public void setTrustScore(Integer trustScore) {
		this.trustScore = trustScore;
	}

	public Integer getSimilarityScore() {
		return similarityScore;
	}

	public void setSimilarityScore(Integer similarityScore) {
		this.similarityScore = similarityScore;
	}
}