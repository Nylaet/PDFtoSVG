package com.genico.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.*;

@Component  // Делаем Spring Bean
public class PdfPointerExtractor extends PDFTextStripper {
    private final Pattern pattern;
    private List<Map<String, Object>> pointersList;  // Храним найденные указатели

    public PdfPointerExtractor() throws IOException {
        super();
        this.pattern = Pattern.compile("[A-Z]+\\d+"); // Универсальный поиск меток
    }

    public List<Map<String, Object>> extractPointers(String pdfPath) throws IOException {
        pointersList = new ArrayList<>(); // Инициализируем список перед обработкой

        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            this.setSortByPosition(true);
            this.writeText(document, new StringWriter());  // Запускаем анализ текста
        }

        return pointersList;
    }

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String pointer = matcher.group();
            TextPosition pos = textPositions.get(matcher.start());

            Map<String, Object> pointerData = new HashMap<>();
            pointerData.put("pointer", pointer);
            pointerData.put("x", pos.getXDirAdj());
            pointerData.put("y", pos.getYDirAdj());
            pointerData.put("page", getCurrentPageNo());

            pointersList.add(pointerData); // Теперь список доступен
        }
    }

    public void saveToJson(List<Map<String, Object>> data, String filePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(new File(filePath), data);
    }
}
