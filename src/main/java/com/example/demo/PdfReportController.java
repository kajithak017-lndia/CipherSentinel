package com.example.demo;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
public class PdfReportController {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private AnomalyRepository anomalyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/download-report/{docId}")
    public ResponseEntity<byte[]> downloadReport(
        @PathVariable int docId,
        Authentication auth) throws Exception {

        User user = userRepository
            .findByUsername(auth.getName());

        Document pdfDoc = documentRepository
            .findById(docId).orElse(null);

        if (pdfDoc == null) {
            return ResponseEntity
                .notFound().build();
        }

        // The user who actually uploaded this document (may differ from the
        // person downloading the report, e.g. a manager reviewing someone else's file)
        User uploader = userRepository
            .findById(pdfDoc.getUploadedBy())
            .orElse(user);

        List<Anomaly> anomalies = anomalyRepository
            .findByDocumentId(docId);

        // Banking service / application this document belongs to (may be null for
        // legacy/uncategorized documents uploaded before applications existed)
        LoanApplication application = pdfDoc.getApplication();

       

        // Create PDF
        ByteArrayOutputStream baos =
            new ByteArrayOutputStream();

        com.itextpdf.text.Document pdf =
            new com.itextpdf.text.Document(
                PageSize.A4, 40, 40, 50, 50);

        PdfWriter.getInstance(pdf, baos);
        pdf.open();

        // ===== FONTS =====
        Font titleFont = new Font(
            Font.FontFamily.HELVETICA, 22,
            Font.BOLD,
            new BaseColor(0, 212, 255));

        Font headFont = new Font(
            Font.FontFamily.HELVETICA, 13,
            Font.BOLD,
            new BaseColor(0, 212, 255));

        Font normalFont = new Font(
            Font.FontFamily.HELVETICA, 10,
            Font.NORMAL,
            new BaseColor(50, 50, 80));

        Font boldFont = new Font(
            Font.FontFamily.HELVETICA, 10,
            Font.BOLD,
            new BaseColor(30, 30, 60));

        Font safeFont = new Font(
            Font.FontFamily.HELVETICA, 12,
            Font.BOLD,
            new BaseColor(0, 200, 100));

        Font unsafeFont = new Font(
            Font.FontFamily.HELVETICA, 12,
            Font.BOLD,
            new BaseColor(220, 50, 70));

        Font highFont = new Font(
            Font.FontFamily.HELVETICA, 9,
            Font.BOLD,
            new BaseColor(220, 50, 70));

        Font medFont = new Font(
            Font.FontFamily.HELVETICA, 9,
            Font.BOLD,
            new BaseColor(200, 140, 0));

        Font lowFont = new Font(
            Font.FontFamily.HELVETICA, 9,
            Font.BOLD,
            new BaseColor(0, 180, 80));

        // ===== HEADER =====
        Paragraph title = new Paragraph(
            "CIPHERSENTINEL", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        pdf.add(title);

        Paragraph sub = new Paragraph(
            "AI-Powered Document Integrity Report",
            new Font(Font.FontFamily.HELVETICA,
                11, Font.NORMAL,
                new BaseColor(100, 100, 140)));
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(6);
        pdf.add(sub);

        // Divider line
        pdf.add(new Paragraph(
            "─────────────────────────────────────────────",
            new Font(Font.FontFamily.HELVETICA,
                8, Font.NORMAL,
                new BaseColor(0, 212, 255))));

        pdf.add(Chunk.NEWLINE);

        // Generated date
        String genTime = LocalDateTime.now()
            .format(DateTimeFormatter
                .ofPattern("dd-MM-yyyy HH:mm:ss"));

        Paragraph genDate = new Paragraph(
            "Report Generated: " + genTime,
            new Font(Font.FontFamily.HELVETICA,
                9, Font.ITALIC,
                new BaseColor(120, 120, 160)));
        genDate.setAlignment(Element.ALIGN_RIGHT);
        pdf.add(genDate);

        pdf.add(Chunk.NEWLINE);

        // ===== APPLICATION DETAILS (only shown if this document belongs to a banking application) =====
        if (application != null) {

            Paragraph appHead = new Paragraph(
                "BANKING APPLICATION", headFont);
            appHead.setSpacingAfter(8);
            pdf.add(appHead);

            PdfPTable appTable = new PdfPTable(2);
            appTable.setWidthPercentage(100);
            appTable.setWidths(new float[]{1f, 2f});
            appTable.setSpacingAfter(16);

            addTableRow(appTable,
                "Banking Service",
                application.getService() != null
                    ? application.getService().getServiceName()
                    : "—",
                boldFont, normalFont);

            addTableRow(appTable,
                "Application Number",
                application.getApplicationNumber() != null
                    ? application.getApplicationNumber()
                    : "—",
                boldFont, normalFont);

            addTableRow(appTable,
                "Application Status",
                application.getStatus() != null
                    ? application.getStatus()
                    : "—",
                boldFont, normalFont);

            pdf.add(appTable);
        }

        // ===== DOCUMENT DETAILS =====
        Paragraph docHead = new Paragraph(
            "DOCUMENT DETAILS", headFont);
        docHead.setSpacingAfter(8);
        pdf.add(docHead);

        PdfPTable docTable = new PdfPTable(2);
        docTable.setWidthPercentage(100);
        docTable.setWidths(new float[]{1f, 2f});
        docTable.setSpacingAfter(16);

        addTableRow(docTable,
            "Document ID",
            String.valueOf(pdfDoc.getId()),
            boldFont, normalFont);

        addTableRow(docTable,
            "File Name",
            pdfDoc.getFileName(),
            boldFont, normalFont);

        addTableRow(docTable,
            "Document Type",
            pdfDoc.getDocumentType() != null
                ? pdfDoc.getDocumentType()
                    .replace("_", " ")
                : "—",
            boldFont, normalFont);

        addTableRow(docTable,
            "Upload Time",
            pdfDoc.getUploadTime() != null
                ? pdfDoc.getUploadTime().format(
                    DateTimeFormatter.ofPattern(
                        "dd-MM-yyyy HH:mm:ss"))
                : "—",
            boldFont, normalFont);

        addTableRow(docTable,
            "Uploaded By",
            uploader != null
                ? uploader.getUsername()
                : "—",
            boldFont, normalFont);

        pdf.add(docTable);

        // ===== SAFETY STATUS =====
        Paragraph statusHead = new Paragraph(
            "SAFETY STATUS", headFont);
        statusHead.setSpacingAfter(8);
        pdf.add(statusHead);

        // Safety Score
        int issueCount = anomalies.size();

        int safeScore =
            pdfDoc.getTrustScore() != null
                ? pdfDoc.getTrustScore()
                : 0;

        PdfPTable statusTable = new PdfPTable(2);
        statusTable.setWidthPercentage(100);
        statusTable.setWidths(new float[]{1f, 2f});
        statusTable.setSpacingAfter(16);

        addTableRow(statusTable,
            "Overall Status",
            pdfDoc.getStatus() != null
                ? pdfDoc.getStatus() : "PENDING",
            boldFont,
            "SAFE".equals(pdfDoc.getStatus())
                ? safeFont : unsafeFont);

        addTableRow(statusTable,
            "Safety Score",
            safeScore + " / 100",
            boldFont, normalFont);

        addTableRow(statusTable,
            "Total Issues Found",
            String.valueOf(issueCount),
            boldFont, normalFont);

        long highCount = anomalies.stream()
            .filter(a -> "HIGH"
                .equals(a.getSeverity()))
            .count();
        long medCount = anomalies.stream()
            .filter(a -> "MEDIUM"
                .equals(a.getSeverity()))
            .count();
        long lowCount = anomalies.stream()
            .filter(a -> "LOW"
                .equals(a.getSeverity()))
            .count();

        addTableRow(statusTable,
            "High Severity Issues",
            String.valueOf(highCount),
            boldFont, highFont);

        addTableRow(statusTable,
            "Medium Severity Issues",
            String.valueOf(medCount),
            boldFont, medFont);

        addTableRow(statusTable,
            "Low Severity Issues",
            String.valueOf(lowCount),
            boldFont, lowFont);

        pdf.add(statusTable);

        // ===== DETECTED ISSUES =====
        Paragraph issueHead = new Paragraph(
            "DETECTED ISSUES", headFont);
        issueHead.setSpacingAfter(8);
        pdf.add(issueHead);

        if (anomalies.isEmpty()) {
            Paragraph noIssue = new Paragraph(
                "✓ No anomalies detected. Document is SAFE.",
                safeFont);
            noIssue.setSpacingAfter(16);
            pdf.add(noIssue);
        } else {
            PdfPTable issueTable =
                new PdfPTable(4);
            issueTable.setWidthPercentage(100);
            issueTable.setWidths(
                new float[]{0.5f, 2f, 1f, 3f});
            issueTable.setSpacingAfter(16);

            // Header row
            String[] headers = {
                "#", "Issue Type",
                "Severity", "Description"
            };
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(
                    new Phrase(h, new Font(
                        Font.FontFamily.HELVETICA,
                        9, Font.BOLD,
                        BaseColor.WHITE)));
                cell.setBackgroundColor(
                    new BaseColor(0, 100, 140));
                cell.setPadding(7);
                cell.setBorderColor(
                    new BaseColor(0, 180, 220));
                issueTable.addCell(cell);
            }

            // Issue rows
            int idx = 1;
            for (Anomaly a : anomalies) {
                BaseColor rowBg = idx % 2 == 0
                    ? new BaseColor(245, 248, 255)
                    : BaseColor.WHITE;

                PdfPCell numCell = new PdfPCell(
                    new Phrase(String.valueOf(idx),
                        normalFont));
                numCell.setPadding(6);
                numCell.setBackgroundColor(rowBg);
                issueTable.addCell(numCell);

                PdfPCell typeCell = new PdfPCell(
                    new Phrase(
                        a.getAnomalyType() != null
                            ? a.getAnomalyType()
                                .replace("_", " ")
                            : "—",
                        boldFont));
                typeCell.setPadding(6);
                typeCell.setBackgroundColor(rowBg);
                issueTable.addCell(typeCell);

                Font sevFont = normalFont;
                if ("HIGH".equals(a.getSeverity()))
                    sevFont = highFont;
                else if ("MEDIUM"
                    .equals(a.getSeverity()))
                    sevFont = medFont;
                else if ("LOW"
                    .equals(a.getSeverity()))
                    sevFont = lowFont;

                PdfPCell sevCell = new PdfPCell(
                    new Phrase(
                        a.getSeverity() != null
                            ? a.getSeverity()
                            : "—",
                        sevFont));
                sevCell.setPadding(6);
                sevCell.setBackgroundColor(rowBg);
                issueTable.addCell(sevCell);

                PdfPCell descCell = new PdfPCell(
                    new Phrase(
                        a.getDescription() != null
                            ? a.getDescription()
                            : "—",
                        normalFont));
                descCell.setPadding(6);
                descCell.setBackgroundColor(rowBg);
                issueTable.addCell(descCell);

                idx++;
            }
            pdf.add(issueTable);
        }

        // ===== AUDIT INFORMATION =====
        Paragraph auditHead = new Paragraph(
            "AUDIT INFORMATION", headFont);
        auditHead.setSpacingAfter(8);
        pdf.add(auditHead);

        
        List<AuditLog> docLogs =
        	    auditLogRepository.findByDocumentId(docId);
        if (docLogs.isEmpty()) {
            pdf.add(new Paragraph(
                "No audit logs available.",
                normalFont));
        } else {
            PdfPTable auditTable =
                new PdfPTable(3);
            auditTable.setWidthPercentage(100);
            auditTable.setWidths(
                new float[]{1.5f, 2f, 2f});
            auditTable.setSpacingAfter(16);

            String[] auditHeaders = {
                "Action", "Details", "Timestamp"
            };
            for (String h : auditHeaders) {
                PdfPCell cell = new PdfPCell(
                    new Phrase(h, new Font(
                        Font.FontFamily.HELVETICA,
                        9, Font.BOLD,
                        BaseColor.WHITE)));
                cell.setBackgroundColor(
                    new BaseColor(0, 100, 140));
                cell.setPadding(7);
                auditTable.addCell(cell);
            }

            int aIdx = 0;
            for (AuditLog log : docLogs) {
                if (aIdx >= 10) break;
                BaseColor rowBg = aIdx % 2 == 0
                    ? new BaseColor(245, 248, 255)
                    : BaseColor.WHITE;

                PdfPCell actionCell =
                    new PdfPCell(new Phrase(
                        log.getAction() != null
                            ? log.getAction()
                                .replace("_", " ")
                            : "—",
                        boldFont));
                actionCell.setPadding(6);
                actionCell.setBackgroundColor(rowBg);
                auditTable.addCell(actionCell);

                PdfPCell detailCell =
                    new PdfPCell(new Phrase(
                        log.getDetails() != null
                            ? log.getDetails()
                            : "—",
                        normalFont));
                detailCell.setPadding(6);
                detailCell.setBackgroundColor(rowBg);
                auditTable.addCell(detailCell);

                PdfPCell timeCell =
                    new PdfPCell(new Phrase(
                        log.getTimestamp() != null
                            ? log.getTimestamp()
                                .format(DateTimeFormatter
                                .ofPattern(
                                    "dd-MM-yyyy HH:mm"))
                            : "—",
                        normalFont));
                timeCell.setPadding(6);
                timeCell.setBackgroundColor(rowBg);
                auditTable.addCell(timeCell);

                aIdx++;
            }
            pdf.add(auditTable);
        }

        // ===== FOOTER =====
        pdf.add(new Paragraph(
            "─────────────────────────────────────────────",
            new Font(Font.FontFamily.HELVETICA,
                8, Font.NORMAL,
                new BaseColor(0, 212, 255))));

        Paragraph footer = new Paragraph(
            "This report was generated by " +
            "CipherSentinel — AI-Powered " +
            "Document Integrity Detection System\n" +
            "Report generated on: " + genTime,
            new Font(Font.FontFamily.HELVETICA,
                8, Font.ITALIC,
                new BaseColor(120, 120, 160)));
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(6);
        pdf.add(footer);

        pdf.close();

        // Return as download
        byte[] pdfBytes = baos.toByteArray();

        HttpHeaders responseHeaders =
            new HttpHeaders();
        responseHeaders.setContentType(
            MediaType.APPLICATION_PDF);
        responseHeaders.setContentDisposition(
            ContentDisposition.builder("attachment")
                .filename("CipherSentinel_Report_" +
                    docId + ".pdf")
                .build());
        auditLogService.log(
        	    auth.getName(),
        	    "REPORT_DOWNLOADED",
        	    "Downloaded report for: "
        	        + pdfDoc.getFileName(),
        	    pdfDoc,
        	    null
        	);

        return ResponseEntity.ok()
            .headers(responseHeaders)
            .body(pdfBytes);
    }

    // Helper method
    private void addTableRow(
        PdfPTable table,
        String label, String value,
        Font labelFont, Font valueFont) {

        PdfPCell labelCell =
            new PdfPCell(new Phrase(label, labelFont));
        labelCell.setPadding(7);
        labelCell.setBackgroundColor(
            new BaseColor(240, 244, 255));
        labelCell.setBorderColor(
            new BaseColor(200, 210, 230));
        table.addCell(labelCell);

        PdfPCell valueCell =
            new PdfPCell(new Phrase(value, valueFont));
        valueCell.setPadding(7);
        valueCell.setBorderColor(
            new BaseColor(200, 210, 230));
        table.addCell(valueCell);
    }
}