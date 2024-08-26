package com.example.demo.controller;

import com.example.demo.FirstBootStatus;
import com.example.demo.service.BddEditor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class FirstBootController {

    @Autowired
    private BddEditor bddEditor;

    @Autowired
    private FirstBootStatus firstBootStatus;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/firstboot")
    public String firstBootPage() {
        if (!firstBootStatus.isFirstBootCompleted()) {
            // Verificar si la base de datos ya existe
            try {
                // Comprueba si existe una tabla clave (puedes cambiar el nombre por una tabla relevante para tu aplicación)
                String sql = "SELECT 1 FROM YOUR_TABLE LIMIT 1"; // Reemplaza "TABLE_NAME" con una tabla clave real
                jdbcTemplate.queryForObject(sql, Integer.class);

                // Si la tabla existe, marcamos el first boot como completado
                firstBootStatus.setFirstBootCompleted(true);
                return "redirect:/login"; // Redirigir al login si ya está completado
            } catch (Exception e) {
                // Si la tabla no existe, continuamos con el flujo normal
                return "firstboot";
            }
        }
        return "redirect:/login";
    }

    @PostMapping("/firstboot/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, Model model) {
        if (file.isEmpty()) {
            model.addAttribute("error", "No se seleccionó ningún archivo.");
            return "firstboot";
        }

        String fileName = file.getOriginalFilename();
        try {
            if (fileName != null) {
                if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
                    bddEditor.setupExcelBdd(file);
                } else if (fileName.endsWith(".sql")) {
                    bddEditor.setupSqlBdd(file);
                } else {
                    model.addAttribute("error", "Formato de archivo no compatible. Por favor, cargue un archivo Excel o SQL.");
                    return "firstboot";
                }

                firstBootStatus.setFirstBootCompleted(true);
                return "redirect:/";
            } else {
                model.addAttribute("error", "Error al procesar el archivo. Intente nuevamente.");
                return "firstboot";
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error al procesar el archivo: " + e.getMessage());
            e.printStackTrace(); // Esto imprimirá el stack trace completo en la consola
            return "firstboot";
        }
    }
}