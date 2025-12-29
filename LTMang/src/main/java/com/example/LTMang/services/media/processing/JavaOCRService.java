package com.example.LTMang.services.media.processing;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import java.awt.image.BufferedImage;
import java.io.File;

public class JavaOCRService {
    private Tesseract tesseract;

    public JavaOCRService() {
        tesseract = new Tesseract();

        String dataPath = new File("assets/tessdata").getAbsolutePath();
        System.out.println("?? Loading OCR Model from: " + dataPath);
        
        tesseract.setDatapath(dataPath);
        tesseract.setLanguage("vie"); 

    }

    public String performOCR(BufferedImage image) {
        try {
            long start = System.currentTimeMillis();
            String result = tesseract.doOCR(image);
            System.out.println("? OCR Time: " + (System.currentTimeMillis() - start) + "ms");
            return result.trim();
        } catch (TesseractException e) {
            System.err.println("? OCR Error: " + e.getMessage());
            return "Lỗi Server OCR: " + e.getMessage();
        }
    }
}

