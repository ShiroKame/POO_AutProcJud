package com.example.demo.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;

public class BddEditor {

    public void agregarRadicado() {
        String radicadoFinal = "";

        // Crear un nuevo BufferedReader en cada llamada a agregarRadicado
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.print("Ingrese el numero de radicado: ");
            String numRad = reader.readLine();

            // Filtrar los dígitos de la entrada
            for (char c : numRad.toCharArray()) {
                if (Character.isDigit(c)) {
                    radicadoFinal += c;
                }
            }

            if (radicadoFinal.length() == 23) {
                // Usa una ruta absoluta para el archivo Excel
                File excelFile = new File("E:/Desktop/OLLAMA/Libro.xlsx");

                try (FileInputStream fis = new FileInputStream(excelFile);
                     Workbook workbook = new XSSFWorkbook(fis)) {

                    Sheet sheet = workbook.getSheetAt(0); // Asume que los datos están en la primera hoja
                    Row headerRow = sheet.getRow(0);
                    int radicadoColumnIndex = -1;

                    // Encuentra el índice de la columna "RADICADO"
                    for (Cell cell : headerRow) {
                        if (cell.getStringCellValue().trim().equalsIgnoreCase("RADICADO")) {
                            radicadoColumnIndex = cell.getColumnIndex();
                            break;
                        }
                    }

                    if (radicadoColumnIndex != -1) {
                        // Añade radicadoFinal a la última fila de la columna
                        int lastRowNum = sheet.getLastRowNum();
                        Row newRow = sheet.createRow(++lastRowNum);
                        Cell newCell = newRow.createCell(radicadoColumnIndex);
                        newCell.setCellValue(radicadoFinal);

                        // Guarda los cambios en el archivo
                        try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                            workbook.write(fos);
                        }

                        // Añade radicadoFinal al archivo de texto
                        try (FileWriter fw = new FileWriter("numeros_de_radicados.txt", true);
                             BufferedWriter bw = new BufferedWriter(fw)) {
                            bw.write(radicadoFinal + "\n");
                            System.out.println("Añadiendo al txt");
                        }

                    } else {
                        System.out.println("No existe la columna \"RADICADO\" en el archivo");
                    }

                } catch (IOException e) {
                    System.out.println("Error al leer o escribir en el archivo Excel: " + e.getMessage());
                }

            } else {
                System.out.println("No se puede registrar el número digitado, ya que debería tener 23 cifras");
            }

        } catch (IOException e) {
            System.out.println("Error al leer la entrada del usuario: " + e.getMessage());
        }
    }
}
