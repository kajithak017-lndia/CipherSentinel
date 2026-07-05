🛡️ CipherSentinel

AI-Powered Document Integrity Detection & Banking Application System

📌 Overview

CipherSentinel is a secure document verification, anomaly detection, and banking application processing platform developed to identify document tampering, integrity violations, suspicious modifications, and missing critical information — while also managing end-to-end banking service applications from customer submission through Officer and Manager approval.

The system analyzes a wide range of document types, including:

* 🪪 PAN Card
* 🆔 Aadhaar Card
* 💰 Salary Slip
* 🏦 Bank Statement
* 📜 Land Record
* 🚗 Vehicle RC
* 📄 Income Certificate
* 🏠 Address Proof
* 📷 Photograph
* 🧾 ITR Returns
* 🎓 Admission Letter
* 📁 Other supporting documents

using OCR, SHA-256 verification, similarity analysis, anomaly detection, audit logging, and automated email notifications.


✨ Key Features

Document Intelligence
* 📄 Document Upload & Verification
* 🔍 OCR-Based Text Extraction (Tesseract)
* 🔐 SHA-256 Integrity Verification
* 📊 Similarity Score Analysis Against Reference Templates
* 🚨 AI-Assisted Anomaly Detection (Weka ML Pattern Scanning)
* 🛡️ Trust Score Calculation
* 🔄 Document Re-Scanning with Tamper Detection
* 📥 Document Preview & Download

Banking Application Workflow
* 🏦 Multi-Service Banking Applications (Home Loan, Personal Loan, Vehicle Loan, Education Loan, Gold Loan, KYC Update, Account Opening)
* 📋 Dynamic Required-Document Checklists Per Service
* 📈 Real-Time Application Progress Tracking (documents uploaded / completion %)
* ❌ Application Cancellation (with full document & anomaly cleanup)
* ✅ Application Submission for Review
* 🧑‍💼 Officer Review Dashboard — claim-based unassigned application queue
* 📊 Manager Final-Approval Dashboard — claim-based officer-endorsed queue
* 📝 Officer & Manager Remarks Tracked Per Application
* 📧 Automated Email Notifications (unsafe document alerts, tampering alerts, application decisions)

Reporting & Compliance
* 📑 PDF Report Generation Per Document (iText)
* 📜 Full Audit Trail Monitoring (uploads, deletes, rescans, logins, role changes, application submissions/cancellations)
* 📤 CSV Audit Log Export (personal and admin-wide)

Access & Administration
* 👤 User Authentication & Authorization
* 🔑 Role-Based Access Control — USER, OFFICER, MANAGER, ADMIN
* 🏠 Shared Customer Access — Officers and Managers retain full customer capabilities (apply, upload, view documents) in addition to their own review dashboards
* 🖥️ Dedicated Dashboards Per Role (Admin Dashboard, Officer Dashboard, Manager Dashboard), each restricted to its own role exactly like Admin's dashboard
* 👤 Profile Management with Photo Upload
* 📈 Dashboard Analytics (document type breakdown, status breakdown, severity breakdown)


🛠 Technologies Used

Backend
* Java 21
* Spring Boot 3.5
* Spring Security
* Spring Data JPA
* Thymeleaf

Database
* MySQL

AI & Analysis
* Weka
* Apache Tika
* Tesseract OCR

Documents & Reporting
* iText (PDF generation)
* Apache POI (DOCX parsing)

Security
* SHA-256 Hash Verification
* BCrypt Password Encoding
* CSRF Protection

Frontend
* HTML
* CSS
* JavaScript
* Bootstrap
* Chart.js


📂 Project Structure

CipherSentinel
│
├── src/
│   ├── main/
│   │   ├── java/com/example/demo
│   │   ├── resources/templates
│   │   ├── resources/static
│   │   ├── resources/samples      (reference document templates)
│   │   └── application.properties
│
├── database/
│   └── ciphersentinel_schema.sql
│
├── screenshots/
│
├── pom.xml
├── README.md
└── .gitignore

⚙️ Prerequisites

Install the following before running the project:

* Java JDK 21
* Maven 3.9+
* MySQL Server
* Spring Tool Suite (STS) / Eclipse / IntelliJ IDEA
* Git

🗄️ Database Setup

Create Database

CREATE DATABASE ciphersentinel;

Configure application.properties

spring.datasource.url=jdbc:mysql://localhost:3306/ciphersentinel
spring.datasource.username=root
spring.datasource.password=your_password

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

📧 Email Configuration

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password

spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

🔍 Tesseract OCR Setup

Install Tesseract OCR:

https://github.com/UB-Mannheim/tesseract/wiki

Configure Tesseract path in the project:

tesseract.setDatapath("C:/Program Files/Tesseract-OCR/tessdata");


🚀 Running the Project

Clone Repository

git clone https://github.com/YOUR_USERNAME/CipherSentinel.git


Move into Project

cd CipherSentinel


Build Project


mvn clean install


Run Application

mvn spring-boot:run


Open Browser

http://localhost:8080



👤 User Roles

* USER — Applies for banking services, uploads documents, tracks application status
* OFFICER — First-level reviewer; claims and reviews submitted applications, verifies documents, approves/rejects and forwards to Manager. Retains full USER capabilities.
* MANAGER — Final approver; claims officer-endorsed applications, makes the final approve/reject decision. Retains full USER capabilities. Also has Audit Trail access.
* ADMIN — Manages users, assigns roles, views system-wide statistics, exports all audit logs.



📄 Supported Document Types

* PAN Card
* Aadhaar Card
* Salary Slip
* Bank Statement
* Land Record
* Vehicle RC
* Income Certificate
* Address Proof
* Photograph
* ITR Returns
* Admission Letter
* Other supporting documents


🏦 Supported Banking Services

* Home Loan
* Personal Loan
* Vehicle Loan
* Education Loan
* Gold Loan
* KYC Update
* Account Opening

Each service defines its own required-document checklist, dynamically presented to the customer at upload time.


🔄 Application Review Flow

1. Customer applies for a banking service and uploads the required documents
2. Customer submits the completed application for review
3. Officer claims the application from the unclaimed queue, reviews documents and trust scores, then approves (forwarding to Manager) or rejects
4. Manager claims the officer-approved application, makes the final decision, and the customer is notified by email
5. Every step is recorded in the Audit Trail


📸 Project Screenshots

### Dashboard
![Dashboard](screenshots/dashboard.png)

### Upload Page
![Upload](screenshots/upload.png)

### Documents Page
![Documents](screenshots/documents.png)

### Anomalies Dashboard
![Anomalies](screenshots/anomalies.png)

### Audit Trail
![Audit](screenshots/audit.png)

### Profile Page
![Profile](screenshots/profile.png)

### Admin Dashboard
![Admin](screenshots/admindashboard1.png)

🚀 Future Enhancements

* Image Tampering Detection via Error Level Analysis (ELA)
* PAN / Aadhaar Checksum Validation
* Salary Slip Arithmetic Cross-Verification
* Additional Document Category Scanners (GST Returns, Cheque/DD, Power of Attorney)
* Advanced AI Detection Models
* Real-Time Monitoring
* Cloud Deployment Support
* Multi-Language OCR Support



👨‍💻 Developer

KAJITHA K

Software Engineer | Web Developer

📧 Email: kajithak017@gmail.com

LinkedIn:

https://www.linkedin.com/in/kajitha-k-729889308


🛡️ CipherSentinel © 2026

Secure • Intelligent • Transparent