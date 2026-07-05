package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.apache.tika.Tika;
import java.io.File;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;

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
	// ===== SALARY SLIP SCANNER =====
	private List<Anomaly> scanSalarySlip(Document doc, String content, String template) {

	    List<Anomaly> anomalies = new ArrayList<>();

	    String[] requiredFields = {
	        "employee name", "employee id", "designation", "basic salary",
	        "gross salary", "net salary", "deductions", "pan",
	        "company name", "pay period", "authorized signatory"
	    };

	    for (String field : requiredFields) {
	        if (!content.contains(field.toLowerCase())) {
	            anomalies.add(createAnomaly(doc.getId(), "MISSING_FIELD", "HIGH",
	                    "Required field missing: '" + field + "' not found in salary slip!"));
	        }
	    }

	    int matchScore = calculateSimilarity(content, template);
	    doc.setSimilarityScore(matchScore);
	    System.out.println("SALARY SLIP SIMILARITY = " + matchScore + "%");

	    if (matchScore < 40) {
	        anomalies.add(createAnomaly(doc.getId(), "LOW_TEMPLATE_MATCH", "HIGH",
	                "Document structure is " + matchScore + "% similar to a valid salary slip. Possible forgery!"));
	    }

	    // Numeric consistency check — net salary should equal gross minus deductions where extractable
	    anomalies.addAll(checkSalaryArithmetic(doc, content));

	    String[] tamperWords = { "forged", "fake", "duplicate", "altered", "tampered", "manipulated", "fabricated" };
	    for (String word : tamperWords) {
	        if (content.contains(word)) {
	            anomalies.add(createAnomaly(doc.getId(), "TAMPERING_DETECTED", "HIGH",
	                    "Suspicious keyword '" + word + "' found — salary slip may be tampered!"));
	        }
	    }

	    return anomalies;
	}

	// ===== SALARY ARITHMETIC CROSS-CHECK =====
	// Bank's own note: "no process to verify pay slip except checking bank statement."
	// Until cross-document checking exists, we at least verify internal arithmetic consistency —
	// a common tell when someone edits just the "net salary" line without adjusting deductions.
	private List<Anomaly> checkSalaryArithmetic(Document doc, String content) {

	    List<Anomaly> anomalies = new ArrayList<>();

	    try {
	        java.util.regex.Pattern grossPattern =
	            java.util.regex.Pattern.compile("gross salary[:\\s]*([\\d,]+\\.?\\d*)");
	        java.util.regex.Pattern deductionPattern =
	            java.util.regex.Pattern.compile("(?:total )?deductions[:\\s]*([\\d,]+\\.?\\d*)");
	        java.util.regex.Pattern netPattern =
	            java.util.regex.Pattern.compile("net salary[:\\s]*([\\d,]+\\.?\\d*)");

	        java.util.regex.Matcher grossMatcher = grossPattern.matcher(content);
	        java.util.regex.Matcher deductionMatcher = deductionPattern.matcher(content);
	        java.util.regex.Matcher netMatcher = netPattern.matcher(content);

	        if (grossMatcher.find() && deductionMatcher.find() && netMatcher.find()) {

	            double gross = Double.parseDouble(grossMatcher.group(1).replace(",", ""));
	            double deductions = Double.parseDouble(deductionMatcher.group(1).replace(",", ""));
	            double net = Double.parseDouble(netMatcher.group(1).replace(",", ""));

	            double expectedNet = gross - deductions;

	            if (Math.abs(expectedNet - net) > 1.0) {
	                anomalies.add(createAnomaly(doc.getId(), "ARITHMETIC_MISMATCH", "HIGH",
	                        "Net salary (" + net + ") does not match Gross (" + gross +
	                        ") minus Deductions (" + deductions + ") = " + expectedNet +
	                        " — figures may have been altered!"));
	            }
	        }

	    } catch (Exception e) {
	        System.out.println("Salary arithmetic check skipped: " + e.getMessage());
	    }

	    return anomalies;
	}

	// ===== PAN CARD SCANNER =====
	private List<Anomaly> scanPanCard(Document doc, String content, String template) {

	    List<Anomaly> anomalies = new ArrayList<>();

	    String[] requiredFields = {
	        "income tax department", "permanent account number", "name",
	        "father's name", "date of birth", "pan"
	    };

	    for (String field : requiredFields) {
	        if (!content.contains(field.toLowerCase())) {
	            anomalies.add(createAnomaly(doc.getId(), "MISSING_FIELD", "HIGH",
	                    "Required field missing: '" + field + "' not found in PAN card!"));
	        }
	    }

	    int matchScore = calculateSimilarity(content, template);
	    doc.setSimilarityScore(matchScore);
	    System.out.println("PAN CARD SIMILARITY = " + matchScore + "%");

	    if (matchScore < 40) {
	        anomalies.add(createAnomaly(doc.getId(), "LOW_TEMPLATE_MATCH", "HIGH",
	                "Document structure is " + matchScore + "% similar to a valid PAN card. Possible forgery!"));
	    }

	    String[] tamperWords = { "forged", "fake", "duplicate", "altered", "tampered", "fabricated" };
	    for (String word : tamperWords) {
	        if (content.contains(word)) {
	            anomalies.add(createAnomaly(doc.getId(), "TAMPERING_DETECTED", "HIGH",
	                    "Suspicious keyword '" + word + "' found — PAN card may be tampered!"));
	        }
	    }

	    return anomalies;
	}

	// ===== AADHAAR CARD SCANNER =====
	private List<Anomaly> scanAadhaarCard(Document doc, String content, String template) {

	    List<Anomaly> anomalies = new ArrayList<>();

	    String[] requiredFields = {
	        "unique identification authority", "aadhaar", "name",
	        "date of birth", "gender", "address"
	    };

	    for (String field : requiredFields) {
	        if (!content.contains(field.toLowerCase())) {
	            anomalies.add(createAnomaly(doc.getId(), "MISSING_FIELD", "HIGH",
	                    "Required field missing: '" + field + "' not found in Aadhaar card!"));
	        }
	    }

	    int matchScore = calculateSimilarity(content, template);
	    doc.setSimilarityScore(matchScore);
	    System.out.println("AADHAAR CARD SIMILARITY = " + matchScore + "%");

	    if (matchScore < 40) {
	        anomalies.add(createAnomaly(doc.getId(), "LOW_TEMPLATE_MATCH", "HIGH",
	                "Document structure is " + matchScore + "% similar to a valid Aadhaar card. Possible forgery!"));
	    }

	    String[] tamperWords = { "forged", "fake", "duplicate", "altered", "tampered", "fabricated" };
	    for (String word : tamperWords) {
	        if (content.contains(word)) {
	            anomalies.add(createAnomaly(doc.getId(), "TAMPERING_DETECTED", "HIGH",
	                    "Suspicious keyword '" + word + "' found — Aadhaar card may be tampered!"));
	        }
	    }

	    return anomalies;
	}

	// ===== BANK STATEMENT SCANNER =====
	private List<Anomaly> scanBankStatement(Document doc, String content, String template) {

	    List<Anomaly> anomalies = new ArrayList<>();

	    String[] requiredFields = {
	        "statement of account", "account holder", "account no",
	        "ifsc", "opening balance", "closing balance"
	    };

	    for (String field : requiredFields) {
	        if (!content.contains(field.toLowerCase())) {
	            anomalies.add(createAnomaly(doc.getId(), "MISSING_FIELD", "HIGH",
	                    "Required field missing: '" + field + "' not found in bank statement!"));
	        }
	    }

	    int matchScore = calculateSimilarity(content, template);
	    doc.setSimilarityScore(matchScore);
	    System.out.println("BANK STATEMENT SIMILARITY = " + matchScore + "%");

	    if (matchScore < 40) {
	        anomalies.add(createAnomaly(doc.getId(), "LOW_TEMPLATE_MATCH", "HIGH",
	                "Document structure is " + matchScore + "% similar to a valid bank statement. Possible forgery!"));
	    }

	    String[] tamperWords = { "forged", "fake", "duplicate", "altered", "tampered", "fabricated" };
	    for (String word : tamperWords) {
	        if (content.contains(word)) {
	            anomalies.add(createAnomaly(doc.getId(), "TAMPERING_DETECTED", "HIGH",
	                    "Suspicious keyword '" + word + "' found — bank statement may be tampered!"));
	        }
	    }

	    return anomalies;
	}

	// ===== VEHICLE RC SCANNER =====
	private List<Anomaly> scanVehicleRC(Document doc, String content, String template) {

	    List<Anomaly> anomalies = new ArrayList<>();

	    String[] requiredFields = {
	        "registration certificate", "registration no", "owner name",
	        "chassis no", "engine no", "vehicle class"
	    };

	    for (String field : requiredFields) {
	        if (!content.contains(field.toLowerCase())) {
	            anomalies.add(createAnomaly(doc.getId(), "MISSING_FIELD", "HIGH",
	                    "Required field missing: '" + field + "' not found in vehicle RC!"));
	        }
	    }

	    int matchScore = calculateSimilarity(content, template);
	    doc.setSimilarityScore(matchScore);
	    System.out.println("VEHICLE RC SIMILARITY = " + matchScore + "%");

	    if (matchScore < 40) {
	        anomalies.add(createAnomaly(doc.getId(), "LOW_TEMPLATE_MATCH", "HIGH",
	                "Document structure is " + matchScore + "% similar to a valid vehicle RC. Possible forgery!"));
	    }

	    String[] tamperWords = { "forged", "fake", "duplicate", "altered", "tampered", "fabricated" };
	    for (String word : tamperWords) {
	        if (content.contains(word)) {
	            anomalies.add(createAnomaly(doc.getId(), "TAMPERING_DETECTED", "HIGH",
	                    "Suspicious keyword '" + word + "' found — vehicle RC may be tampered!"));
	        }
	    }

	    return anomalies;
	}

	// ===== INCOME CERTIFICATE SCANNER =====
	private List<Anomaly> scanIncomeCertificate(Document doc, String content, String template) {

	    List<Anomaly> anomalies = new ArrayList<>();

	    String[] requiredFields = {
	        "income certificate", "annual income", "certificate no", "issued by"
	    };

	    for (String field : requiredFields) {
	        if (!content.contains(field.toLowerCase())) {
	            anomalies.add(createAnomaly(doc.getId(), "MISSING_FIELD", "HIGH",
	                    "Required field missing: '" + field + "' not found in income certificate!"));
	        }
	    }

	    int matchScore = calculateSimilarity(content, template);
	    doc.setSimilarityScore(matchScore);
	    System.out.println("INCOME CERTIFICATE SIMILARITY = " + matchScore + "%");

	    if (matchScore < 40) {
	        anomalies.add(createAnomaly(doc.getId(), "LOW_TEMPLATE_MATCH", "HIGH",
	                "Document structure is " + matchScore + "% similar to a valid income certificate. Possible forgery!"));
	    }

	    String[] tamperWords = { "forged", "fake", "duplicate", "altered", "tampered", "fabricated" };
	    for (String word : tamperWords) {
	        if (content.contains(word)) {
	            anomalies.add(createAnomaly(doc.getId(), "TAMPERING_DETECTED", "HIGH",
	                    "Suspicious keyword '" + word + "' found — income certificate may be tampered!"));
	        }
	    }

	    return anomalies;
	}

	// ===== ADDRESS PROOF SCANNER =====
	private List<Anomaly> scanAddressProof(Document doc, String content, String template) {

	    List<Anomaly> anomalies = new ArrayList<>();

	    String[] requiredFields = {
	        "consumer name", "service address", "billing period", "bill amount"
	    };

	    for (String field : requiredFields) {
	        if (!content.contains(field.toLowerCase())) {
	            anomalies.add(createAnomaly(doc.getId(), "MISSING_FIELD", "HIGH",
	                    "Required field missing: '" + field + "' not found in address proof!"));
	        }
	    }

	    int matchScore = calculateSimilarity(content, template);
	    doc.setSimilarityScore(matchScore);
	    System.out.println("ADDRESS PROOF SIMILARITY = " + matchScore + "%");

	    if (matchScore < 40) {
	        anomalies.add(createAnomaly(doc.getId(), "LOW_TEMPLATE_MATCH", "HIGH",
	                "Document structure is " + matchScore + "% similar to a valid address proof. Possible forgery!"));
	    }

	    String[] tamperWords = { "forged", "fake", "duplicate", "altered", "tampered", "fabricated" };
	    for (String word : tamperWords) {
	        if (content.contains(word)) {
	            anomalies.add(createAnomaly(doc.getId(), "TAMPERING_DETECTED", "HIGH",
	                    "Suspicious keyword '" + word + "' found — address proof may be tampered!"));
	        }
	    }

	    return anomalies;
	}

	// ===== ITR RETURNS SCANNER =====
	private List<Anomaly> scanItrReturns(Document doc, String content, String template) {

	    List<Anomaly> anomalies = new ArrayList<>();

	    String[] requiredFields = {
	        "income tax return", "acknowledgement", "assessment year",
	        "gross total income", "taxable income"
	    };

	    for (String field : requiredFields) {
	        if (!content.contains(field.toLowerCase())) {
	            anomalies.add(createAnomaly(doc.getId(), "MISSING_FIELD", "HIGH",
	                    "Required field missing: '" + field + "' not found in ITR returns!"));
	        }
	    }

	    int matchScore = calculateSimilarity(content, template);
	    doc.setSimilarityScore(matchScore);
	    System.out.println("ITR RETURNS SIMILARITY = " + matchScore + "%");

	    if (matchScore < 40) {
	        anomalies.add(createAnomaly(doc.getId(), "LOW_TEMPLATE_MATCH", "HIGH",
	                "Document structure is " + matchScore + "% similar to a valid ITR return. Possible forgery!"));
	    }

	    String[] tamperWords = { "forged", "fake", "duplicate", "altered", "tampered", "fabricated" };
	    for (String word : tamperWords) {
	        if (content.contains(word)) {
	            anomalies.add(createAnomaly(doc.getId(), "TAMPERING_DETECTED", "HIGH",
	                    "Suspicious keyword '" + word + "' found — ITR return may be tampered!"));
	        }
	    }

	    return anomalies;
	}

	// ===== ADMISSION LETTER SCANNER =====
	private List<Anomaly> scanAdmissionLetter(Document doc, String content, String template) {

	    List<Anomaly> anomalies = new ArrayList<>();

	    String[] requiredFields = {
	        "offer of admission", "course", "academic year", "registrar"
	    };

	    for (String field : requiredFields) {
	        if (!content.contains(field.toLowerCase())) {
	            anomalies.add(createAnomaly(doc.getId(), "MISSING_FIELD", "HIGH",
	                    "Required field missing: '" + field + "' not found in admission letter!"));
	        }
	    }

	    int matchScore = calculateSimilarity(content, template);
	    doc.setSimilarityScore(matchScore);
	    System.out.println("ADMISSION LETTER SIMILARITY = " + matchScore + "%");

	    if (matchScore < 40) {
	        anomalies.add(createAnomaly(doc.getId(), "LOW_TEMPLATE_MATCH", "HIGH",
	                "Document structure is " + matchScore + "% similar to a valid admission letter. Possible forgery!"));
	    }

	    String[] tamperWords = { "forged", "fake", "duplicate", "altered", "tampered", "fabricated" };
	    for (String word : tamperWords) {
	        if (content.contains(word)) {
	            anomalies.add(createAnomaly(doc.getId(), "TAMPERING_DETECTED", "HIGH",
	                    "Suspicious keyword '" + word + "' found — admission letter may be tampered!"));
	        }
	    }

	    return anomalies;
	}

	// ===== ERROR LEVEL ANALYSIS (ELA) — OFFLINE IMAGE TAMPER DETECTION =====
	// Re-compresses the image at a known quality and compares against the original.
	// Regions that were edited after the original save show a different error level
	// than untouched regions — a standard offline forensic technique, no internet needed.
	private List<Anomaly> runELACheck(Document doc, String uploadedFilePath) {

	    List<Anomaly> anomalies = new ArrayList<>();

	    try {
	        File originalFile = new File(uploadedFilePath);

	        if (!originalFile.getName().toLowerCase().matches(".*\\.(jpg|jpeg)$")) {
	            // ELA is most reliable on JPEG due to its compression artifacts.
	            return anomalies;
	        }

	        java.awt.image.BufferedImage original = javax.imageio.ImageIO.read(originalFile);

	        if (original == null) {
	            return anomalies;
	        }

	        // Re-save at fixed quality (90%) into memory
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        javax.imageio.ImageWriter writer = javax.imageio.ImageIO.getImageWritersByFormatName("jpg").next();
	        javax.imageio.stream.ImageOutputStream ios = javax.imageio.ImageIO.createImageOutputStream(baos);
	        writer.setOutput(ios);

	        javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
	        param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
	        param.setCompressionQuality(0.90f);

	        writer.write(null, new javax.imageio.IIOImage(original, null, null), param);
	        writer.dispose();
	        ios.close();

	        java.awt.image.BufferedImage recompressed =
	            javax.imageio.ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));

	        int width = original.getWidth();
	        int height = original.getHeight();

	        long totalDiff = 0;
	        long maxRegionDiff = 0;
	        int blockSize = 16;
	        int suspiciousBlocks = 0;
	        int totalBlocks = 0;

	        for (int by = 0; by < height; by += blockSize) {
	            for (int bx = 0; bx < width; bx += blockSize) {

	                long blockDiff = 0;
	                int pixelsInBlock = 0;

	                for (int y = by; y < Math.min(by + blockSize, height); y++) {
	                    for (int x = bx; x < Math.min(bx + blockSize, width); x++) {

	                        int p1 = original.getRGB(x, y);
	                        int p2 = recompressed.getRGB(x, y);

	                        int r1 = (p1 >> 16) & 0xff, g1 = (p1 >> 8) & 0xff, b1 = p1 & 0xff;
	                        int r2 = (p2 >> 16) & 0xff, g2 = (p2 >> 8) & 0xff, b2 = p2 & 0xff;

	                        blockDiff += Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2);
	                        pixelsInBlock++;
	                    }
	                }

	                double avgBlockDiff = pixelsInBlock == 0 ? 0 : (double) blockDiff / pixelsInBlock;
	                totalDiff += blockDiff;
	                totalBlocks++;

	                if (avgBlockDiff > maxRegionDiff) {
	                    maxRegionDiff = (long) avgBlockDiff;
	                }

	                // A block with unusually high error relative to typical noise stands out —
	                // threshold tuned conservatively to avoid false positives on scans/photos.
	                if (avgBlockDiff > 45) {
	                    suspiciousBlocks++;
	                }
	            }
	        }

	        double suspiciousRatio = totalBlocks == 0 ? 0 : (double) suspiciousBlocks / totalBlocks;

	        System.out.println("ELA — suspicious blocks: " + suspiciousBlocks + "/" + totalBlocks +
	                " (" + String.format("%.1f", suspiciousRatio * 100) + "%)");

	        if (suspiciousRatio > 0.08) {
	            anomalies.add(createAnomaly(doc.getId(), "ELA_TAMPER_DETECTED", "HIGH",
	                    "Error Level Analysis found " + String.format("%.1f", suspiciousRatio * 100) +
	                    "% of the image with abnormal compression artifacts — " +
	                    "consistent with localized editing (e.g. altered text, swapped photo, or changed figures)."));
	        } else if (suspiciousRatio > 0.03) {
	            anomalies.add(createAnomaly(doc.getId(), "ELA_MINOR_INCONSISTENCY", "MEDIUM",
	                    "Error Level Analysis found minor compression inconsistencies (" +
	                    String.format("%.1f", suspiciousRatio * 100) + "%) — recommend manual review."));
	        }

	    } catch (Exception e) {
	        System.out.println("ELA check skipped: " + e.getMessage());
	    }

	    return anomalies;
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

				if (fileType.equals("text/plain")) {

					content = new String(fileContent);

					content = content.toLowerCase();

					System.out.println("TXT CONTENT START");
					System.out.println(content);
					System.out.println("TXT CONTENT END");

				} else {

					Tika tika = new Tika();

					content = tika.parseToString(new ByteArrayInputStream(fileContent)).toLowerCase();

					System.out.println("PDF CONTENT START");
					System.out.println(content);
					System.out.println("PDF CONTENT END");
				}
				if (fileType.contains("wordprocessingml")) {

					XWPFDocument wordDoc = new XWPFDocument(new ByteArrayInputStream(fileContent));

					XWPFWordExtractor extractor = new XWPFWordExtractor(wordDoc);

					content = extractor.getText().toLowerCase();

					System.out.println("DOCX CONTENT START");
					System.out.println(content);
					System.out.println("DOCX CONTENT END");

					extractor.close();
					wordDoc.close();
				}

				if (content == null || content.trim().isEmpty()) {

					content = "empty_document";
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
		String salarySlipTemplate = loadTemplate("salary_slip_sample.txt");

		// New templates for additional document categories
		String panCardTemplate = loadTemplate("pan_card_sample.txt");
		String aadhaarCardTemplate = loadTemplate("aadhaar_card_sample.txt");
		String bankStatementTemplate = loadTemplate("bank_statement_sample.txt");
		String vehicleRcTemplate = loadTemplate("vehicle_rc_sample.txt");
		String incomeCertificateTemplate = loadTemplate("income_certificate_sample.txt");
		String addressProofTemplate = loadTemplate("address_proof_sample.txt");
		String itrReturnsTemplate = loadTemplate("itr_returns_sample.txt");
		String admissionLetterTemplate = loadTemplate("admission_letter_sample.txt");

		if ("SALARY_SLIP".equalsIgnoreCase(document.getDocumentType())) {
		    anomalies.addAll(scanSalarySlip(document, content, salarySlipTemplate));
		}

		// ✅ ELA runs for ANY image upload, regardless of document type —
		// material alteration can happen to a photographed/scanned salary slip,
		// PAN card, Aadhaar, or any other hard-copy document.
		if (isImage) {
		    anomalies.addAll(runELACheck(document, uploadedFilePath));
		}
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

		if ("PAN_CARD".equalsIgnoreCase(document.getDocumentType())) {

			anomalies.addAll(scanPanCard(document, content, panCardTemplate));
		}

		if ("AADHAAR_CARD".equalsIgnoreCase(document.getDocumentType())) {

			anomalies.addAll(scanAadhaarCard(document, content, aadhaarCardTemplate));
		}

		if ("BANK_STATEMENT".equalsIgnoreCase(document.getDocumentType())) {

			anomalies.addAll(scanBankStatement(document, content, bankStatementTemplate));
		}

		if ("VEHICLE_RC".equalsIgnoreCase(document.getDocumentType())) {

			anomalies.addAll(scanVehicleRC(document, content, vehicleRcTemplate));
		}

		if ("INCOME_CERTIFICATE".equalsIgnoreCase(document.getDocumentType())) {

			anomalies.addAll(scanIncomeCertificate(document, content, incomeCertificateTemplate));
		}

		if ("ADDRESS_PROOF".equalsIgnoreCase(document.getDocumentType())) {

			anomalies.addAll(scanAddressProof(document, content, addressProofTemplate));
		}

		if ("ITR_RETURNS".equalsIgnoreCase(document.getDocumentType())) {

			anomalies.addAll(scanItrReturns(document, content, itrReturnsTemplate));
		}

		if ("ADMISSION_LETTER".equalsIgnoreCase(document.getDocumentType())) {

			anomalies.addAll(scanAdmissionLetter(document, content, admissionLetterTemplate));
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
		String[] requiredFields = { "financial statement", "assets", "liabilities", "owner", "total assets",
				"total liabilities", "property valorization", "income from property", "declaration" };
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
		if (matchScore < 25) {
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