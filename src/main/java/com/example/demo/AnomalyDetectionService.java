package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.apache.tika.Tika;
import java.io.File;

@Service
public class AnomalyDetectionService {

	@Autowired
	private AnomalyRepository anomalyRepository;
	@Autowired
	private OCRService ocrService;

	// LOAD SAMPLE TEMPLATES
	private String loadTemplate(String fileName) {
		try {
			ClassPathResource resource = new ClassPathResource("samples/" + fileName);
			byte[] bytes = Files.readAllBytes(resource.getFile().toPath());
			return new String(bytes).toLowerCase();
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	// MAIN SCAN METHOD
	public List<Anomaly> scanDocument(Document document, byte[] fileContent, String uploadedFilePath) {
		List<Anomaly> anomalies = new ArrayList<>();
		String content = "";

		String fileType = document.getFileType() == null ? "" : document.getFileType().toLowerCase();

		boolean isImage = fileType.startsWith("image");
		try {

			if (isImage) {

				File imageFile = new File(uploadedFilePath);

				System.out.println("OCR File = " + imageFile.getAbsolutePath());

				System.out.println("OCR Exists = " + imageFile.exists());

				System.out.println("OCR Length = " + imageFile.length());

				content = ocrService.extractText(uploadedFilePath);

				System.out.println("OCR CONTENT START");

				System.out.println(content);

				System.out.println("OCR CONTENT END");
			} else {

				Tika tika = new Tika();

				content = tika.parseToString(new ByteArrayInputStream(fileContent)).toLowerCase();
				System.out.println("PDF CONTENT START");
				System.out.println(content);
				System.out.println("PDF CONTENT END");
				if (content == null || content.trim().isEmpty()) {

					throw new RuntimeException("Uploaded file contains no readable content.");
				}
			}

		} catch (Exception e) {

			e.printStackTrace();

			content = "";
		}
		System.out.println("File Type = " + fileType);

		System.out.println("Is Image = " + isImage);

		// Load templates
		String landTemplate = loadTemplate("land_record_sample.txt");

		String legalTemplate = loadTemplate("legal_document_sample.txt");

		String financialTemplate = loadTemplate("financial_statement_sample.txt");
		System.out.println("DOCUMENT TYPE = " + document.getDocumentType());
		System.out.println("FILE NAME = " + document.getFileName());

		// SCAN BASED ON DOCUMENT TYPE
		if ("LAND_RECORD".equalsIgnoreCase(document.getDocumentType())) {

			anomalies.addAll(scanLandRecord(document, content, landTemplate));

		}

		if ("LEGAL_DOCUMENT".equalsIgnoreCase(document.getDocumentType())) {

			anomalies.addAll(scanLegalDocument(document, content, legalTemplate));
		}

		if ("FINANCIAL_STATEMENT".equalsIgnoreCase(document.getDocumentType())) {

			anomalies.addAll(scanFinancialStatement(document, content, financialTemplate));
		}

		// Common Checks
		anomalies.addAll(commonChecks(document, content));

		// ML Checks
		anomalies.addAll(wekaPatternScan(document, content));

		// Trust Score
		int trustScore = 100;

		for (Anomaly a : anomalies) {

			if ("HIGH".equalsIgnoreCase(a.getSeverity())) {

				trustScore -= 40;

			} else if ("MEDIUM".equalsIgnoreCase(a.getSeverity())) {

				trustScore -= 15;

			} else if ("LOW".equalsIgnoreCase(a.getSeverity())) {

				trustScore -= 5;
			}
		}

		if (trustScore < 0) {
			trustScore = 0;
		}
		boolean hasHighIssue = false;

		for (Anomaly a : anomalies) {

			if ("HIGH".equalsIgnoreCase(a.getSeverity())) {

				hasHighIssue = true;
				break;
			}
		}

		if (hasHighIssue) {

			document.setStatus("UNSAFE");

		} else if (trustScore >= 80) {

			document.setStatus("SAFE");

		} else if (trustScore >= 60) {

			document.setStatus("SUSPICIOUS");

		} else {

			document.setStatus("UNSAFE");
		}

		System.out.println("Trust Score = " + trustScore);
		document.setTrustScore(trustScore);
		System.out.println("FINAL TRUST SCORE = " + trustScore);

		System.out.println("FINAL STATUS = " + document.getStatus());

		return anomalies;
	}

	// ===== LAND RECORD SCANNER =====
	private List<Anomaly> scanLandRecord(Document doc, String content, String template) {

		List<Anomaly> anomalies = new ArrayList<>();

		// Required fields from template
		String[] requiredFields = { "statement of immovable property", "survey no", "description of property",
				"area of land", "date of acquisition", "value of property", "ownership", "property details",
				"declaration", "signature of the declarant", "notary public" };
		for (String field : requiredFields) {

			if (!content.contains(field.toLowerCase())) {

				anomalies.add(createAnomaly(doc.getId(), "MISSING_FIELD", "HIGH",
						"Required field missing: '" + field + "' not found in land record!"));
			}
		}

		// Check template similarity
		int matchScore = calculateSimilarity(content, template);

		doc.setSimilarityScore(matchScore);

		System.out.println("LAND SIMILARITY = " + matchScore + "%");

		if (matchScore < 40) {

			anomalies.add(createAnomaly(doc.getId(), "LOW_TEMPLATE_MATCH", "HIGH",
					"Document structure is " + matchScore + "% similar to valid land record. Possible forgery!"));
		}

		// Tampering keywords
		String[] tamperWords = { "forged", "fake", "duplicate", "altered", "tampered", "unauthorized", "illegal",
				"void", "cancelled" };
		for (String word : tamperWords) {
			if (content.contains(word)) {
				anomalies.add(createAnomaly(doc.getId(), "TAMPERING_DETECTED", "HIGH",
						"Suspicious keyword '" + word + "' found — document may be tampered!"));
			}
		}

		return anomalies;
	}

	// ===== LEGAL DOCUMENT SCANNER =====
	private List<Anomaly> scanLegalDocument(Document doc, String content, String template) {

		List<Anomaly> anomalies = new ArrayList<>();

		// Required fields from template
		String[] requiredFields = { "ownership declaration", "affiant details", "statement of ownership", "survey no",
				"verification", "deponent signature", "notary public", "aadhaar", "property details" };
		for (String field : requiredFields) {

			if (!content.contains(field.toLowerCase())) {

				anomalies.add(createAnomaly(doc.getId(), "MISSING_FIELD", "HIGH",
						"Required field missing: '" + field + "' not found in legal document!"));
			}
		}

		// Check template similarity
		int matchScore = calculateSimilarity(content, template);
		doc.setSimilarityScore(matchScore);
		System.out.println("LEGAL SIMILARITY = " + matchScore + "%");
		if (matchScore < 40) {
			anomalies.add(createAnomaly(doc.getId(), "LOW_TEMPLATE_MATCH", "HIGH", "Document structure is " + matchScore
					+ "% similar to valid legal document. " + "Possible forgery!"));
		}

		// Forgery keywords
		String[] forgeryWords = { "forged", "fake", "illegal", "unauthorized", "tampered", "altered", "fabricated",
				"counterfeit" };
		for (String word : forgeryWords) {
			if (content.contains(word)) {
				anomalies.add(createAnomaly(doc.getId(), "FORGERY_DETECTED", "HIGH",
						"Forgery keyword '" + word + "' detected in legal document!"));
			}
		}

		return anomalies;
	}

	// ===== FINANCIAL STATEMENT SCANNER =====
	private List<Anomaly> scanFinancialStatement(Document doc, String content, String template) {

		List<Anomaly> anomalies = new ArrayList<>();

		// Required fields from template
		String[] requiredFields = { "financial statement", "assets", "liabilities", "owner's equity", "total assets",
				"total liabilities", "property valorization", "income from property", "declaration",
				"signature of owner" };
		for (String field : requiredFields) {

			if (!content.contains(field.toLowerCase())) {

				anomalies.add(createAnomaly(doc.getId(), "MISSING_FIELD", "HIGH",
						"Required field missing: '" + field + "' not found in financial record!"));
			}
		}

		// Check template similarity
		int matchScore = calculateSimilarity(content, template);

		doc.setSimilarityScore(matchScore);
		System.out.println("FINANCIAL SIMILARITY = " + matchScore + "%");
		if (matchScore < 40) {
			anomalies.add(createAnomaly(doc.getId(), "LOW_TEMPLATE_MATCH", "HIGH", "Document structure is " + matchScore
					+ "% similar to valid financial statement. " + "Possible fraud!"));
		}

		// Fraud keywords
		String[] fraudWords = { "fraud", "fake", "forged", "manipulated", "falsified", "tampered", "unauthorized" };
		for (String word : fraudWords) {
			if (content.contains(word)) {
				anomalies.add(createAnomaly(doc.getId(), "FRAUD_DETECTED", "HIGH",
						"Fraud keyword '" + word + "' found in financial statement!"));
			}
		}

		return anomalies;
	}

	// ===== COMMON CHECKS =====
	private List<Anomaly> commonChecks(Document doc, String content) {

		List<Anomaly> anomalies = new ArrayList<>();

		// Empty document
		if (content.trim().length() < 50) {
			anomalies.add(
					createAnomaly(doc.getId(), "EMPTY_DOCUMENT", "HIGH", "Document is empty or too small — invalid!"));
		}

		// Repeated content
		String[] lines = content.split("\n");
		Set<String> uniqueLines = new HashSet<>(Arrays.asList(lines));
		if (lines.length > 5 && uniqueLines.size() < lines.length / 2) {
			anomalies.add(createAnomaly(doc.getId(), "REPEATED_CONTENT", "MEDIUM",
					"Document has too many repeated lines — " + "possible copy-paste manipulation!"));
		}

		return anomalies;
	}

	// ===== WEKA ML PATTERN SCAN =====
	private List<Anomaly> wekaPatternScan(Document doc, String content) {

		List<Anomaly> anomalies = new ArrayList<>();

		try {
			// Calculate document features
			int wordCount = content.split("\\s+").length;
			int lineCount = content.split("\n").length;
			int specialCharCount = content.replaceAll("[a-zA-Z0-9\\s]", "").length();
			double specialCharRatio = (double) specialCharCount / content.length();

			// Weka-style pattern rules
			// Rule 1 — Too few words
			if (wordCount < 20) {
				anomalies.add(createAnomaly(doc.getId(), "ML_LOW_WORD_COUNT", "MEDIUM",
						"ML Pattern: Only " + wordCount + " words found — insufficient content!"));
			}

			// Rule 2 — Too many special characters
			if (specialCharRatio > 0.3) {
				anomalies.add(createAnomaly(doc.getId(), "ML_SUSPICIOUS_CHARACTERS", "HIGH",
						"ML Pattern: " + String.format("%.0f", specialCharRatio * 100) + "% special characters — "
								+ "suspicious document structure!"));
			}

			// Rule 3 — Too few lines
			if (lineCount < 5) {
				anomalies.add(createAnomaly(doc.getId(), "ML_LOW_LINE_COUNT", "MEDIUM",
						"ML Pattern: Only " + lineCount + " lines — document too short!"));
			}

			// Rule 4 — No numbers in document
			System.out.println("NUMBER CHECK = " + content.matches("(?s).*\\d+.*"));
			System.out.println("CONTENT = " + content);

			if (!content.matches("(?s).*\\d+.*")) {

				anomalies.add(createAnomaly(doc.getId(), "ML_NO_NUMBERS", "MEDIUM",
						"ML Pattern: No numbers found — official documents must have numbers!"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return anomalies;
	}

	// ===== TEMPLATE SIMILARITY CALCULATOR =====
	private int calculateSimilarity(String content, String template) {

		content = content.toLowerCase();

		template = template.toLowerCase();

		String[] keywords = template.split("\\s+");

		int matched = 0;
		int total = 0;

		for (String word : keywords) {

			word = word.trim();

			if (word.length() < 5)
				continue;

			total++;

			if (content.contains(word)) {
				matched++;
			}
		}

		if (total == 0)
			return 0;

		return (matched * 100) / total;
	}

	// HELPER METHOD
	private Anomaly createAnomaly(int docId, String type, String severity, String description) {

		Anomaly anomaly = new Anomaly();
		anomaly.setDocumentId(docId);
		anomaly.setAnomalyType(type);
		anomaly.setSeverity(severity);
		anomaly.setDescription(description);
		return anomalyRepository.save(anomaly);
	}

}
