package com.genico.ui;

import com.genico.service.PdfToSvgConverter;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.FileBuffer;
import com.vaadin.flow.router.Route;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@Route("")
public class MainView extends VerticalLayout {

    private final PdfToSvgConverter converter;
    private final TextField savePathField;

    public MainView(PdfToSvgConverter converter) {
        this.converter = converter;
        this.savePathField = new TextField("Папка сохранения");
        savePathField.setValue("G:/Work/TestFiles");

        FileBuffer buffer = new FileBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("application/pdf");

        Button convertButton = new Button("Конвертировать", event -> {
            File uploadedFile = buffer.getFileData().getFile();
            if (uploadedFile == null) {
                Notification.show("Файл не загружен!");
                return;
            }

            String saveDir = savePathField.getValue();
            File saveFolder = new File(saveDir);
            if (!saveFolder.exists()) {
                saveFolder.mkdirs();
            }

            System.out.println("Запуск конвертации: PDF=" + uploadedFile.getAbsolutePath() + " → " + saveDir);
            try {
                converter.convertPdfToSvg(uploadedFile.getAbsolutePath(), saveDir);
                Notification.show("Конвертация завершена! SVG и JSON сохранены в " + saveDir);
            } catch (Exception e) {
                Notification.show("Ошибка конвертации: " + e.getMessage());
            }
        });

        add(savePathField, upload, convertButton);
    }
}
