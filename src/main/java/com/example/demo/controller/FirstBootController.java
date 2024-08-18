package com.example.demo.controller;

import com.example.demo.FirstBootStatus; // Asegúrate de importar la clase FirstBootStatus
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
    private FirstBootStatus firstBootStatus; // Inyección de FirstBootStatus

    @GetMapping("/firstboot")
    public String firstBootPage() {
        // Verifica si el primer arranque se ha completado
        if (firstBootStatus.isFirstBootCompleted()) {
            return "redirect:/login"; // Redirige si ya se completó el primer arranque
        }
        return "firstboot"; // Renderiza la vista firstboot.html
    }

    @PostMapping("/firstboot/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, Model model) {
        String fileName = file.getOriginalFilename();

        if (fileName == null || fileName.isEmpty()) {
            model.addAttribute("error", "No se seleccionó ningún archivo.");
            return "firstboot";
        }

        try {
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

            // Después de procesar el archivo, crear el usuario administrador base
            createDefaultAdminUser();

            // Marcar que el primer arranque se ha completado
            firstBootStatus.setFirstBootCompleted(true);

            // Redirigir a la pantalla de login
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", "Error al procesar el archivo: " + e.getMessage());
            return "firstboot";
        }
    }

    private void createDefaultAdminUser() {
        // Verificar si ya existe un usuario administrador
        if (userRepository.findByUsername("admin").isEmpty()) {
            User adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setPassword(passwordEncoder.encode("admin123")); // Contraseña predeterminada

            Optional<Role> adminRole = roleRepository.findByName("ROLE_ADMIN");
            if (adminRole.isEmpty()) {
                Role role = new Role();
                role.setName("ROLE_ADMIN");
                roleRepository.save(role);
                adminUser.setRoles(Set.of(role));
            } else {
                adminUser.setRoles(Set.of(adminRole.get()));
            }

            userRepository.save(adminUser);
        }
    }
}
