package com.example.demo.service;

import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.hssf.usermodel.HSSFWorkbook; // Para archivos XLS
import org.apache.poi.xssf.usermodel.XSSFWorkbook; // Para archivos XLSX


import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BddEditor {

    public void setupExcelBdd(MultipartFile file) throws IOException {
        if (!file.getOriginalFilename().endsWith(".xlsx") && !file.getOriginalFilename().endsWith(".xls")) {
            throw new IllegalArgumentException("El archivo debe ser un archivo Excel (.xlsx o .xls)");
        }

        try (InputStream is = file.getInputStream();
             Workbook workbook = file.getOriginalFilename().endsWith(".xlsx") ? new XSSFWorkbook(is) : new HSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            int radicadoIndex = -1;
            int columnCount = headerRow.getPhysicalNumberOfCells();
            StringBuilder columnsBuilder = new StringBuilder();
            StringBuilder valuesBuilder = new StringBuilder();
            String[] columnsToExclude = {"Exp"};

            for (int i = 0; i < columnCount; i++) {
                Cell cell = headerRow.getCell(i);
                String columnName = cell.getStringCellValue().trim();

                boolean exclude = false;
                for (String excludeColumn : columnsToExclude) {
                    if (excludeColumn.equalsIgnoreCase(columnName)) {
                        exclude = true;
                        break;
                    }
                }

                if (exclude) {
                    continue;
                }

                if ("RADICADO".equalsIgnoreCase(columnName)) {
                    radicadoIndex = i;
                }

                if (i > 0 && !exclude) {
                    columnsBuilder.append(", ");
                    valuesBuilder.append(", ");
                }
                if (!exclude) {
                    columnsBuilder.append(columnName);
                    valuesBuilder.append("?");
                }
            }

            if (radicadoIndex == -1) {
                System.out.println("Columna 'RADICADO' no encontrada.");
                return;
            }

            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("ALTER TABLE your_table ADD COLUMN RADICADO VARCHAR(255);\n");

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell radicadoCell = row.getCell(radicadoIndex);
                    if (radicadoCell == null || radicadoCell.toString().trim().isEmpty()) {
                        continue;
                    }

                    StringBuilder values = new StringBuilder();
                    values.append("(");
                    for (int j = 0; j < columnCount; j++) {
                        Cell cell = row.getCell(j);
                        String columnName = headerRow.getCell(j).getStringCellValue().trim();

                        boolean exclude = false;
                        for (String excludeColumn : columnsToExclude) {
                            if (excludeColumn.equalsIgnoreCase(columnName)) {
                                exclude = true;
                                break;
                            }
                        }

                        if (exclude) {
                            continue;
                        }

                        if (j > 0) {
                            values.append(", ");
                        }
                        if (cell != null) {
                            String cellValue = cell.toString().replace("'", "''");
                            values.append("'").append(cellValue).append("'");
                        } else {
                            values.append("NULL");
                        }
                    }
                    values.append(")");

                    sqlBuilder.append("INSERT INTO your_table (").append(columnsBuilder.toString()).append(") VALUES ").append(values.toString()).append(";\n");
                }
            }

            File sqlFile = new File("E:/Desktop/OLLAMA/insert_data.sql");
            try (FileWriter writer = new FileWriter(sqlFile)) {
                writer.write(sqlBuilder.toString());
            }

            System.out.println("Datos exportados a SQL con éxito.");

        }
    }

    @SuppressWarnings("null")
    public void setupSqlBdd(MultipartFile file) throws IOException {
        if (!file.getOriginalFilename().endsWith(".sql")) {
            throw new IllegalArgumentException("El archivo debe ser un archivo SQL (.sql)");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
             FileWriter writer = new FileWriter("E:/Desktop/OLLAMA/processed_data.sql")) {

            String line;
            StringBuilder sqlBuilder = new StringBuilder();
            Pattern insertPattern = Pattern.compile("INSERT INTO\\s+your_table\\s*\\(([^)]+)\\)\\s*VALUES\\s*\\(([^)]+)\\);");

            while ((line = reader.readLine()) != null) {
                Matcher matcher = insertPattern.matcher(line);
                if (matcher.find()) {
                    String columns = matcher.group(1);
                    String values = matcher.group(2);
                    String[] columnArray = columns.split(",\\s*");
                    String[] valueArray = values.split(",\\s*");

                    StringBuilder formattedValues = new StringBuilder();
                    formattedValues.append("(");
                    for (int i = 0; i < columnArray.length; i++) {
                        if (i > 0) {
                            formattedValues.append(", ");
                        }
                        String value = valueArray[i].replace("'", "''"); // Escapar comillas simples
                        formattedValues.append("'").append(value).append("'");
                    }
                    formattedValues.append(")");

                    sqlBuilder.append("INSERT INTO your_table (").append(columns).append(") VALUES ").append(formattedValues).append(";\n");
                }
            }

            writer.write(sqlBuilder.toString());
            System.out.println("Archivo SQL procesado con éxito.");
        }
    }
    public void agregarRadicado(String number){
        System.out.println("accion1 con rad "+number);
    }
}
