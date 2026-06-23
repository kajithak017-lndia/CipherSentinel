package com.example.demo;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)

	private int id;

	private String username;
	private String password;
	private String role;
	private String profileImage;
	private String email;
	private String securityQuestion;
	private String securityAnswer;
	@Column(nullable = false)
	private Boolean anomalyAlerts = true;
	@Column(nullable = false)
	private Boolean auditNotifications = true;
	@Column(nullable = false)
	private Boolean uploadNotifications = true;
	@Column(nullable = false)
	private Boolean twoFactorAuth = false;

	// Getters and Setters
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getProfileImage() {
		return profileImage;
	}

	public void setProfileImage(String profileImage) {
		this.profileImage = profileImage;
	}

	public Boolean getAnomalyAlerts() {
		return anomalyAlerts;
	}

	public void setAnomalyAlerts(Boolean anomalyAlerts) {
		this.anomalyAlerts = anomalyAlerts;
	}

	public Boolean getAuditNotifications() {
		return auditNotifications;
	}

	public void setAuditNotifications(Boolean auditNotifications) {
		this.auditNotifications = auditNotifications;
	}

	public Boolean getUploadNotifications() {
		return uploadNotifications;
	}

	public void setUploadNotifications(Boolean uploadNotifications) {
		this.uploadNotifications = uploadNotifications;
	}

	public Boolean getTwoFactorAuth() {
		return twoFactorAuth;
	}

	public void setTwoFactorAuth(Boolean twoFactorAuth) {
		this.twoFactorAuth = twoFactorAuth;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getSecurityQuestion() {
		return securityQuestion;
	}

	public void setSecurityQuestion(String securityQuestion) {
		this.securityQuestion = securityQuestion;
	}

	public String getSecurityAnswer() {
		return securityAnswer;
	}

	public void setSecurityAnswer(String securityAnswer) {
		this.securityAnswer = securityAnswer;
	}

}
