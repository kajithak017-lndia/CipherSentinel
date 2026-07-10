package com.example.demo;

import java.io.File;

import org.springframework.stereotype.Service;

import net.sourceforge.tess4j.Tesseract;

@Service
public class OCRService {

	public String extractText(String imagePath) {

	    try {

	        Tesseract tesseract = new Tesseract();

	        // Reads from TESSDATA_PREFIX env var (set in Docker/production).
	        // Falls back to the Windows path for local dev on Windows,
	        // and to the standard Linux install location otherwise.
	        String tessDataPath = System.getenv("TESSDATA_PREFIX");
	        if (tessDataPath == null || tessDataPath.isBlank()) {
	            String os = System.getProperty("os.name", "").toLowerCase();
	            tessDataPath = os.contains("win")
	                    ? "C:\\Program Files\\Tesseract-OCR\\tessdata"
	                    : "/usr/share/tesseract-ocr/5/tessdata";
	        }
	        tesseract.setDatapath(tessDataPath);

	        tesseract.setLanguage("eng");
			String text = tesseract.doOCR(new File(imagePath));

			System.out.println("OCR TEXT = \n" + text);

			return text == null ? "" : text.toLowerCase();
		} catch (Exception e) {

			e.printStackTrace();

			return "";
		}
	}
}