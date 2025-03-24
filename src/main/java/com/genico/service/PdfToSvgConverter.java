package com.genico.service;

import org.apache.batik.svggen.GenericImageHandler;
import org.apache.batik.svggen.ImageHandlerBase64Encoder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.DefaultImageHandler;
import org.apache.batik.svggen.SVGGraphics2D;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfToSvgConverter {
    private static final Logger log = LoggerFactory.getLogger(PdfToSvgConverter.class);
    private static final String POINTER_REGEX = "^[A-Za-z]+\\d+$";

    public void convertPdfToSvg(String pdfPath, String saveDir) {
        log.info("Начинаем конвертацию: PDF={} → SVG={}, JSON={}", pdfPath, saveDir);

        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 300, ImageType.RGB);

            String baseName = new File(pdfPath).getName();
            if (baseName.contains(".")) {
                baseName = baseName.substring(0, baseName.lastIndexOf('.'));
            }

            Files.createDirectories(Paths.get(saveDir)); // Создание директории

            File svgFile = new File(saveDir, baseName + ".svg");
            File jsonFile = new File(saveDir, baseName + ".json");

            String svgContent = generateSvg(image);
            saveToFile(svgFile, svgContent);

            JSONObject json = extractPointers(svgContent);
            saveToFile(jsonFile, json.toString(4));

            log.info("Конвертация завершена! SVG и JSON сохранены: {}, {}", svgFile.getAbsolutePath(), jsonFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Ошибка при обработке PDF", e);
            throw new RuntimeException("Ошибка при обработке PDF: " + e.getMessage(), e);
        }
    }

    private String generateSvg(BufferedImage image) {
        StringWriter writer = new StringWriter();

        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
        Document document = domImpl.createDocument(null, "svg", null);
        SVGGraphics2D g2d = new SVGGraphics2D(document);
        g2d.setSVGCanvasSize(new Dimension(image.getWidth(), image.getHeight()));

        g2d.drawImage(image, 0, 0, null);

        try {
            ImageHandlerBase64Encoder imageHandler = new ImageHandlerBase64Encoder();
            g2d.getGeneratorContext().setImageHandler(imageHandler);
        } catch (Exception e) {
            log.error("Ошибка при установке обработчика изображений", e);
            throw new RuntimeException("Ошибка при установке обработчика изображений", e);
        }


        try {
            g2d.stream(writer, true);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при генерации SVG", e);
        }
        return writer.toString();
    }

    private void saveToFile(File path, String content) {
        if (path == null) {
            log.error("Путь к файлу не может быть null");
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write(content);
        } catch (IOException e) {
            log.error("Ошибка при сохранении файла {}", path, e);
        }
    }

    private JSONObject extractPointers(String svgContent) {
        JSONObject json = new JSONObject();
        json.put("pointers", new JSONArray());

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(svgContent)));

            JSONArray pointersArray = new JSONArray();
            for (Element textElement : getElementsByTag(doc, "text")) {
                String content = textElement.getTextContent().trim();

                if (content.matches(POINTER_REGEX)) {
                    JSONObject pointer = new JSONObject();
                    pointer.put("label", content);

                    String x = null;
                    String y = null;

                    NodeList tspans = textElement.getElementsByTagName("tspan");
                    if (tspans.getLength() > 0) {
                        Element tspan = (Element) tspans.item(0);
                        if (tspan.hasAttribute("x") && tspan.hasAttribute("y")) {
                            x = tspan.getAttribute("x");
                            y = tspan.getAttribute("y");
                            log.debug("Координаты из tspan для '{}': x='{}', y='{}'", content, x, y);
                        }
                    }

                    if (x == null || y == null) {
                        x = textElement.getAttribute("x");
                        y = textElement.getAttribute("y");
                        log.debug("Координаты из text для '{}': x='{}', y='{}'", content, x, y);
                    }

                    if (isNumeric(x) && isNumeric(y)) {
                        pointer.put("x", x);
                        pointer.put("y", y);
                        pointersArray.put(pointer);
                    } else {
                        log.warn("Нечисловые координаты для указателя '{}': x='{}', y='{}'", content, x, y);
                    }
                }
            }

            json.put("pointers", pointersArray);
            log.info("Извлечено {} указателей", pointersArray.length());

        } catch (Exception e) {
            log.error("Ошибка при извлечении указателей", e);
        }

        return json;
    }

    private List<Element> getElementsByTag(Document doc, String tagName) {
        List<Element> elements = new ArrayList<>();
        NodeList nodeList = doc.getElementsByTagName(tagName);
        for (int i = 0; i < nodeList.getLength(); i++) {
            elements.add((Element) nodeList.item(i));
        }
        return elements;
    }

    private boolean isNumeric(String str) {
        if (str == null) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}