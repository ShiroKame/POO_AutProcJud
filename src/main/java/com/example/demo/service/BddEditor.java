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
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.stream.Collectors;
import java.text.ParseException;
import java.util.*;


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
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            String columnName = cell.getStringCellValue().trim();
            if (!columnName.isEmpty()) { // Solo si hay un encabezado
                boolean noData = true; // Inicializamos la bandera
                for (int j = 1; j <= sheet.getLastRowNum(); j++) { // Recorremos las filas
                    Row row = sheet.getRow(j);
                    if (row != null && row.getCell(i) != null) { // Si hay datos en la fila y columna
                        noData = false; // Marcamos que hay datos
                        break; // Salimos del bucle
                    }
                }
                if (!noData) { // Solo si hay datos en la columna
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
            }
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
        String sql = "ALTER TABLE YOUR_TABLE ADD COLUMN estado VARCHAR(1000);UPDATE YOUR_TABLE SET estado = 'activo' WHERE RADICADO IS NOT NULL AND RADICADO <> '';";
        jdbcTemplate.update(sql);
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
        String sql = "INSERT INTO YOUR_TABLE (RADICADO, estado) VALUES (?, 'activo')";
        jdbcTemplate.update(sql, number);
    }
    public void cerrarCaso(String number) {
        String sql = "UPDATE YOUR_TABLE SET estado = 'cerrado' WHERE RADICADO = VALUES (?)";
        jdbcTemplate.update(sql, number);
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
                Solicitud solicitud = new Solicitud(number, "PENDIENTE", "AgregarCaso");
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
    public void aceptarSolicitud(Long id, String tipoSolicitud) throws Exception {
        Optional<Solicitud> solicitud = solicitudRepository.findById(id);
        if (solicitud.isPresent()) {
            Solicitud s = solicitud.get();
            s.setEstado("APROBADO");
            solicitudRepository.save(s);
            if(tipoSolicitud.equals("AgregarCaso")){
                agregarRadicado(solicitud.get().getRadicado());
            }else if(tipoSolicitud.equals("CerrarCaso")){
                cerrarCaso(solicitud.get().getRadicado());
            }else{
                throw new Exception("Tipo de solicitud no válido");
            }
        } else {
            throw new Exception("Solicitud no encontrada");
        }
    }

    // Método para negar una solicitud
    @Transactional
    public void negarSolicitud(Long id, String tipoSolicitud) throws Exception {
        Optional<Solicitud> solicitud = solicitudRepository.findById(id);
        if (solicitud.isPresent()) {
            Solicitud s = solicitud.get();
            s.setEstado("RECHAZADO");
            solicitudRepository.save(s);
        } else {
            throw new Exception("Solicitud no encontrada");
        }
    }
    public List<String> getRadicadosActivos() {
        String sql = "SELECT DISTINCT TRIM(RADICADO) FROM YOUR_TABLE WHERE ESTADO = 'activo'";
        return jdbcTemplate.queryForList(sql, String.class);
    }
    @SuppressWarnings("unchecked")
    public void printAndSaveActions(Map<String, Object> process, String nradicado) {
        boolean radicadoActivo = false;
        List<String> radicadosActivos = getRadicadosActivos();
        for (String radicado : radicadosActivos) {
            if (radicado.equals(nradicado)) {
                radicadoActivo = true;
                break;
            }
        }
        if (radicadoActivo) {
            System.out.println("radicado activo");
            if (process.containsKey("process")) {
                Map<String, Object> processDetails = (Map<String, Object>) process.get("process");
                if (processDetails.containsKey("actions")) {
                    List<Map<String, String>> actions = (List<Map<String, String>>) processDetails.get("actions");
                    
                    // Encontrar la actuación más reciente
                    Map<String, String> latestAction = null;
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    Date latestDate = null;
        
                    for (Map<String, String> action : actions) {
                        String actionDateStr = action.get("action_date");
                        if (actionDateStr != null && !actionDateStr.isEmpty()) {
                            try {
                                Date actionDate = sdf.parse(actionDateStr);
                                if (latestDate == null || actionDate.after(latestDate)) {
                                    latestDate = actionDate;
                                    latestAction = action;
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }
        
                    if (latestAction != null) {
                        String radicado = (String) processDetails.get("key_procces");
                        if (radicado != null) {
                            // Verificar si la tabla existe y comparar datos
                            boolean tableExists = checkIfTableExists(radicado);
                            if (tableExists) {
                                Map<String, String> oldAction = getLatestAction(radicado);
                                
                                // Imprimir datos antiguos y nuevos para comparación
                                System.out.println("Datos antiguos en la base de datos:");
                                if (oldAction != null) {
                                    System.out.println("Action Date: " + oldAction.get("action_date"));
                                    System.out.println("Action: " + oldAction.get("action"));
                                    System.out.println("Annotation: " + oldAction.get("annotation"));
                                    System.out.println("Start Date: " + oldAction.get("start_date"));
                                    System.out.println("End Date: " + oldAction.get("end_date"));
                                    System.out.println("Registration Date: " + oldAction.get("registration_date"));
                                } else {
                                    System.out.println("No se encontraron datos antiguos.");
                                }
        
                                if (oldAction != null && isSameAction(latestAction, oldAction)) {
                                    System.out.println("No han habido cambios.");
                                } else {
                                    saveAction(radicado, latestAction);
                                    System.out.println("La base de datos se actualizó.");
                                }
                            } else {
                                // Crear la tabla y guardar los datos si la tabla no existe
                                createTableIfNotExists(radicado);
                                saveAction(radicado, latestAction);
                                System.out.println("La base de datos se creó y se actualizó.");
                            }
                        }
                    } else {
                        System.out.println("No se encontraron actuaciones.");
                    }
                } else {
                    System.out.println("No se encontraron actuaciones.");
                }
            } else {
                System.out.println("El mapa no contiene la clave 'process'.");
            }
        }else{
            System.out.println("radicado inactivo");
        }
        
    }


    private boolean checkIfTableExists(String radicado) {
        try {
            String sql = "SELECT COUNT(*) AS count FROM information_schema.tables WHERE table_schema = 'PUBLIC' AND table_name = '"+radicado+"'";
            Integer count = Integer.parseInt(jdbcTemplate.queryForList(sql, String.class).get(0));
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, String> getLatestAction(String radicado) {
        try {
            String sql = "SELECT * FROM " + escapeTableName(radicado) + " ORDER BY action_date DESC LIMIT 1";
            return jdbcTemplate.queryForObject(sql, (ResultSet rs, int rowNum) -> {
                Map<String, String> action = new HashMap<>();
                action.put("action_date", rs.getString("action_date"));
                action.put("action", rs.getString("action"));
                action.put("annotation", rs.getString("annotation"));
                action.put("start_date", rs.getString("start_date"));
                action.put("end_date", rs.getString("end_date"));
                action.put("registration_date", rs.getString("registration_date"));
                return action;
            });
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isSameAction(Map<String, String> newAction, Map<String, String> oldAction) {
        return newAction.equals(oldAction);
    }

    private void createTableIfNotExists(String radicado) {
        String sql = "CREATE TABLE IF NOT EXISTS " + escapeTableName(radicado) + " (" +
                     "id INT AUTO_INCREMENT PRIMARY KEY, " +
                     "action_date VARCHAR(255), " +
                     "action VARCHAR(255), " +
                     "annotation VARCHAR(255), " +
                     "start_date VARCHAR(255), " +
                     "end_date VARCHAR(255), " +
                     "registration_date VARCHAR(255))";

        jdbcTemplate.execute(sql);
    }

    private void saveAction(String radicado, Map<String, String> action) {
        String sql = "INSERT INTO " + escapeTableName(radicado) + " (action_date, action, annotation, start_date, end_date, registration_date) VALUES (?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
            action.get("action_date"),
            action.get("action"),
            action.get("annotation"),
            action.get("start_date"),
            action.get("end_date"),
            action.get("registration_date")
        );
    }

    private String escapeTableName(String tableName) {
        // Escapar nombres de tablas para evitar inyección SQL y otros problemas
        return "`" + tableName.replace("`", "``") + "`";
    }
      
}
