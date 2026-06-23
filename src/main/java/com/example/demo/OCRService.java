package com.example.demo;

import java.io.File;

import org.springframework.stereotype.Service;

import net.sourceforge.tess4j.Tesseract;

@Service
public class OCRService {

	public String extractText(String imagePath) {

		try {

			Tesseract tesseract = new Tesseract();

			tesseract.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");

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