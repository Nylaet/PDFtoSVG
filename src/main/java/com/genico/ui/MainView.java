package com.genico.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
public class MainView extends VerticalLayout {

    private TextField filePathField;
    private TextField savePathField;
    private Button convertButton;

    public MainView() {
        setSpacing(true);
        setPadding(true);

        H2 title = new H2("PDF to SVG Converter");

        // Поле для выбора файла
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("application/pdf");

        filePathField = new TextField("Выбранный файл");
        filePathField.setReadOnly(true);

        // Поле для пути сохранения
        savePathField = new TextField("Папка для сохранения");

        // Кнопка конвертации
        convertButton = new Button("Конвертировать", event -> {
            System.out.println("Конвертация запущена...");
        });

        // Добавляем обработку загрузки файла
        upload.addSucceededListener(event -> {
            filePathField.setValue(event.getFileName());
        });

        add(title, upload, filePathField, savePathField, convertButton);
    }
}
