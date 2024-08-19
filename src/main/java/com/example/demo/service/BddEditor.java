package com.example.demo.service;

import com.example.demo.model.Solicitud;
import com.example.demo.repository.SolicitudRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;


import org.apache.poi.ss.usermodel.*;

@Service
public class BddEditor {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Transactional
    public void setupExcelBdd(MultipartFile file) throws IOException {
        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        Row headerRow = sheet.getRow(0);

        List<String> columns = new ArrayList<>();
        for (Cell cell : headerRow) {
            String columnName = cell.getStringCellValue().trim();
            if (columnName.isEmpty()) {
                columnName = "Column_" + cell.getColumnIndex();
            }
            // Reemplazar caracteres no válidos y espacios
            columnName = columnName.replaceAll("[^a-zA-Z0-9_]", "_");
            if (columnName.matches("\\d.*")) {
                columnName = "col_" + columnName;
            }
            columns.add(columnName);
        }

        // Crear la tabla si no existe
        StringBuilder createTableSQL = new StringBuilder("CREATE TABLE IF NOT EXISTS your_table (");
        for (String column : columns) {
            createTableSQL.append(column).append(" VARCHAR(1000), "); // Tamaño configurado a 1000 caracteres
        }
        createTableSQL.setLength(createTableSQL.length() - 2);
        createTableSQL.append(")");

        jdbcTemplate.execute(createTableSQL.toString());

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                StringBuilder insertSQL = new StringBuilder("INSERT INTO your_table (");
                insertSQL.append(String.join(", ", columns));
                insertSQL.append(") VALUES (");

                List<Object> values = new ArrayList<>();
                for (int j = 0; j < columns.size(); j++) {
                    Cell cell = row.getCell(j);
                    if (cell != null) {
                        values.add(cell.toString());
                    } else {
                        values.add(null); // Agregamos null para mantener la sincronización
                    }
                    insertSQL.append("?, "); // Agregamos "?" para cada columna, incluyendo null
                }
                insertSQL.setLength(insertSQL.length() - 2); // Removemos la última coma y espacio
                insertSQL.append(")");

                jdbcTemplate.update(insertSQL.toString(), values.toArray()); // Ejecutamos la consulta
            }
        }
    }

    @Transactional
    public void setupSqlBdd(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sql.append(line);
                if (line.endsWith(";")) {
                    jdbcTemplate.execute(sql.toString());
                    sql.setLength(0);
                }
            }
        }
    }

    @Transactional
    public void agregarRadicado(String number) {

        jdbcTemplate.update("INSERT INTO YOUR_TABLE (RADICADO) VALUES (?)", number);
    }

    public List<String> getTableColumns(String tableName) {
        String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ?";
        return jdbcTemplate.queryForList(sql, String.class, tableName);
    }

    // Método para obtener los datos de la tabla
    public List<Map<String, Object>> getTableData(String tableName) {
        String sql = "SELECT * FROM " + tableName;
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
    
        // Reemplaza los valores null con un espacio en blanco
        List<Map<String, Object>> normalizedResult = result.stream()
            .map(row -> row.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue() != null ? entry.getValue() : " "
                ))
            )
            .collect(Collectors.toList());
    
        // Filtra las filas basándote en todas las columnas excepto EXP_
        return normalizedResult.stream()
            .filter(row -> {
                // Verifica si todas las columnas excepto EXP_ tienen solo espacios en blanco
                boolean allOtherColumnsEmpty = row.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals("EXP_"))
                    .allMatch(entry -> entry.getValue().toString().trim().isEmpty());
                
                // Mantén la fila si no todas las demás columnas están vacías
                return !allOtherColumnsEmpty;
            })
            .collect(Collectors.toList());
    }     
    
    @Transactional
    public void solicitudAgregarRadicado(String number) throws Exception {
        if (number != null && number.length() == 23) {
            String sql = "SELECT COUNT(*) FROM YOUR_TABLE WHERE TRIM(RADICADO) = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, number);

            if (count != null && count > 0) {
                throw new Exception("El radicado '" + number + "' ya existe en la base de datos.");
            } else {
                // Crear y guardar la solicitud en la base de datos
                Solicitud solicitud = new Solicitud(number, "PENDIENTE");
                solicitudRepository.save(solicitud);
                System.out.println("El radicado no existe y la solicitud ha sido guardada para revisión.");
            }
        } else {
            throw new Exception("Radicado incorrecto, debe tener 23 dígitos.");
        }
    }

    @Transactional
    public void printPendingRequests() {
        List<Solicitud> pendingRequests = solicitudRepository.findByEstado("PENDIENTE");
        if (pendingRequests.isEmpty()) {
            System.out.println("No hay solicitudes pendientes.");
        } else {
            System.out.println("Solicitudes pendientes:");
            for (Solicitud solicitud : pendingRequests) {
                System.out.println("ID: " + solicitud.getId() +
                                   ", Radicado: " + solicitud.getRadicado() +
                                   ", Estado: " + solicitud.getEstado());
            }
        }
    }

    // Método para obtener todas las solicitudes pendientes
    public List<Solicitud> getPendientes() {
        return solicitudRepository.findByEstado("PENDIENTE");
    }

    // Método para aceptar una solicitud
    @Transactional
    public void aceptarSolicitud(Long id) throws Exception {
        Optional<Solicitud> solicitud = solicitudRepository.findById(id);
        if (solicitud.isPresent()) {
            Solicitud s = solicitud.get();
            s.setEstado("APROBADO");
            solicitudRepository.save(s);
        } else {
            throw new Exception("Solicitud no encontrada");
        }
    }

    // Método para negar una solicitud
    @Transactional
    public void negarSolicitud(Long id) throws Exception {
        Optional<Solicitud> solicitud = solicitudRepository.findById(id);
        if (solicitud.isPresent()) {
            Solicitud s = solicitud.get();
            s.setEstado("RECHAZADO");
            solicitudRepository.save(s);
        } else {
            throw new Exception("Solicitud no encontrada");
        }
    }
}
