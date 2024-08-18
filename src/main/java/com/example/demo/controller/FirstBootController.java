package com.example.demo.controller;

import com.example.demo.FirstBootStatus;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.BddEditor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestPart;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@Controller
public class FirstBootController {
    private final BddEditor bddEditor = new BddEditor();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
                    // Procesar archivo Excel
                    bddEditor.setupExcelBdd(file);
                } else if (fileName.endsWith(".sql")) {
                    // Ejecutar script SQL
                    bddEditor.setupSqlBdd(file);
                } else {
                    model.addAttribute("error", "Formato de archivo no compatible. Por favor, cargue un archivo Excel o SQL.");
                    return "firstboot";
                }

                // Marcar que el primer arranque se ha completado
                firstBootStatus.setFirstBootCompleted(true);

                // Redirigir a la pantalla de login
                return "redirect:/";
            } else {
                model.addAttribute("error", "Error al procesar el archivo. Intente nuevamente.");
                return "firstboot";
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error al procesar el archivo: " + e.getMessage());
            return "firstboot";
        }
    }
}
