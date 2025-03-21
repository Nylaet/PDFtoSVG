package com.genico.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.xmlgraphics.java2d.svg.SVGGraphics2D;
import org.apache.xmlgraphics.java2d.svg.SVGGeneratorContext;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

import javax.xml.transform.TransformerException;

public class PdfToSvgConverter {

    public void convertPdfToSvg(String pdfPath, String svgPath) throws IOException {
        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 300, ImageType.RGB);

            // Создаем SVG
            String svgContent = generateSvg(image);
            saveToFile(svgPath, svgContent);
        }
    }

    private String generateSvg(BufferedImage image) {
        StringWriter writer = new StringWriter();
        SVGGraphics2D g2d = new SVGGraphics2D(image.getWidth(), image.getHeight());
        g2d.drawImage(image, 0, 0, null);
        try {
            g2d.stream(writer, true);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при генерации SVG", e);
        }
        return writer.toString();
    }

    private void saveToFile(String path, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write(content);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при сохранении SVG", e);
        }
    }
}
