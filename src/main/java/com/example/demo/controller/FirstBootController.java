package com.example.demo.controller;

import com.example.demo.FirstBootStatus;
import com.example.demo.service.BddEditor;
import org.springframework.beans.factory.annotation.Autowired;
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

    @GetMapping("/firstboot")
    public String firstBootPage() {
        if (firstBootStatus.isFirstBootCompleted()) {
            return "redirect:/login";
        }
        return "firstboot";
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