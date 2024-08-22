package com.example.demo.controller;

import com.example.demo.service.BddEditor;
import com.example.demo.service.WebScrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

import com.example.demo.model.Role;
import com.example.demo.model.Solicitud;
import com.example.demo.model.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final BddEditor bddEditor;
    private final WebScrapper webScrapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public AdminController(BddEditor bddEditor, WebScrapper webScrapper) {
        this.bddEditor = bddEditor;
        this.webScrapper = webScrapper;
    }

    @GetMapping
    public String adminHome(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "adminHome"; // Admin-specific view
    }

    @PostMapping("/execute")
    public String executeAction(@RequestParam(name = "action", required = false) String action,
                                @RequestParam(name = "radNumber", required = false) String radNumber,
                                Model model) {
        String result;

        if ("action1".equals(action)) {
            try {
                bddEditor.agregarRadicado(radNumber);
                result = "Acción 1 ejecutada con éxito!";
            } catch (Exception e) {
                result = "Error al ejecutar Acción 1: " + e.getMessage();
            }
        } else if ("action2".equals(action)) {
            try {
                if (radNumber != null && !radNumber.isEmpty()) {
                    Map<String, Object> process = webScrapper.queryProcess(radNumber);
                    model.addAttribute("process", process);
                    result = "Acción 2 ejecutada con éxito!";
                } else {
                    result = "Número de radicado no proporcionado!" ;
                }
            } catch (Exception e) {
                result = "La consulta no generó resultados, por favor revisar las opciones ingresadas e intentarlo nuevamente."+ e.getMessage();
            }
        } else {
            result = "Acción desconocida!";
        }

        model.addAttribute("result", result);
        return "adminHome"; // Return to adminHome view
    }

    @GetMapping("/accesspanel")
    public String accessPanel(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "accesspanel"; // View for the access panel to edit users
    }

    @PostMapping("/create-user")
    public String createUser(@RequestParam String username, @RequestParam String password, @RequestParam String role, Model model) {
        // Verificar si el nombre de usuario ya existe
        Optional<User> existingUser = userRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            model.addAttribute("error", "El nombre de usuario ya existe. Por favor, elija otro.");
            return "redirect:/admin/accesspanel"; // Volver a la vista de edición de usuarios
        }

        // Crear un nuevo usuario
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // Codificar la contraseña

        // Buscar el rol
        Optional<Role> optionalRole = roleRepository.findByName(role);
        if (optionalRole.isEmpty()) {
            throw new RuntimeException("Rol no encontrado");
        }

        Role userRole = optionalRole.get();
        user.setRoles(Set.of(userRole));
        userRepository.save(user);

        return "redirect:/admin/accesspanel";
    }

    @PostMapping("/delete-user")
    public String deleteUser(@RequestParam Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Usuario no encontrado");
        }
        userRepository.deleteById(id);
        return "redirect:/admin/accesspanel";
    }

    @PostMapping("/update-password")
    public String updatePassword(@RequestParam Long id, @RequestParam String newPassword) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado");
        }
        User user = optionalUser.get();
        user.setPassword(passwordEncoder.encode(newPassword)); // Codificar la nueva contraseña
        userRepository.save(user);
        return "redirect:/admin/accesspanel";
    }
    
    //SELECT * FROM YOUR_TABLE 
    @GetMapping("/adminbdd")
    public String viewDatabase(Model model) {
        String tableName = "YOUR_TABLE";
        List<String> columns = bddEditor.getTableColumns(tableName);
        List<Map<String, Object>> rows = bddEditor.getTableData(tableName);

        // Normaliza las claves y los valores, si es necesario
        List<Map<String, Object>> normalizedRows = rows.stream()
            .map(row -> row.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue() != null ? entry.getValue() : " "
                ))
            )
            .collect(Collectors.toList());

        model.addAttribute("columns", columns);
        model.addAttribute("rows", normalizedRows);

        return "adminbdd";
    }
    
    // Método para ver solicitudes pendientes
    @GetMapping("/accesspanel/petitions")
    public String viewPetitions(Model model) {
        List<Solicitud> pendientes = bddEditor.getPendientes();
        model.addAttribute("pendientes", pendientes);
        return "petitions"; // Vista para las solicitudes
    }

    // Método para aceptar una solicitud
    @PostMapping("/accept-petition")
    public String acceptPetition(@RequestParam Long id, Model model) {
        try {
            bddEditor.aceptarSolicitud(id, "AgregarCaso");
            model.addAttribute("result", "Solicitud aceptada con éxito.");
        } catch (Exception e) {
            model.addAttribute("result", "Error al aceptar la solicitud: " + e.getMessage());
        }
        return "redirect:/admin/accesspanel/petitions";
    }

    // Método para negar una solicitud
    @PostMapping("/reject-petition")
    public String rejectPetition(@RequestParam Long id, Model model) {
        try {
            bddEditor.negarSolicitud(id, "AgregarCaso");
            model.addAttribute("result", "Solicitud rechazada con éxito.");
        } catch (Exception e) {
            model.addAttribute("result", "Error al rechazar la solicitud: " + e.getMessage());
        }
        return "redirect:/admin/accesspanel/petitions";
    }
    @PostMapping("/procesar-bdd")
    public String procesarBdd() {
        webScrapper.procesarBdd();
        return "adminHome";
    }
    
    // En su lugar, usamos  para recibir el formulario entero como un JSON.
    @PostMapping("/adminbdd/save")
    public ResponseEntity<String> saveTableData(@RequestBody List<Map<String, String>> tableData) {
        // Procesar los datos de la tabla
        System.out.println("Datos de la tabla recibidos: " + tableData);
        return ResponseEntity.ok("Datos guardados con éxito");
    }
    @PostMapping("/save")
    public ResponseEntity<String> saveTableDataa(@RequestBody List<Map<String, String>> tableData) {
        // Procesar los datos de la tabla
        System.out.println("Datos de la tabla recibidos: " + tableData);
        return ResponseEntity.ok("Datos guardados con éxito");
    }
    @PostMapping("/admin/adminbdd/save")
    public ResponseEntity<String> saveTableDatab(@RequestBody List<Map<String, String>> tableData) {
        // Procesar los datos de la tabla
        System.out.println("Datos de la tabla recibidos: " + tableData);
        return ResponseEntity.ok("Datos guardados con éxito");
    }
    @PostMapping("/admin/schedule-settings")
    public String updateSchedule(
        @RequestParam("scheduledTime") String scheduledTime,
        @RequestParam("daysOfWeek") List<String> daysOfWeek,
        Model model
    ) {
        // Actualizar la configuración del cron en WebScrapper
        webScrapper.updateCronExpression(scheduledTime, daysOfWeek);
        model.addAttribute("successMessage", "Configuración guardada exitosamente.");
        System.out.println("ASDAFGDSF123456");
        return "redirect:/admin/accesspanel"; // Redirige al panel de acceso
    }
    @PostMapping("/schedule-settings")
    public String updateSchedule1(
        @RequestParam("scheduledTime") String scheduledTime,
        @RequestParam("daysOfWeek") List<String> daysOfWeek,
        Model model
    ) {
        // Actualizar la configuración del cron en WebScrapper
        webScrapper.updateCronExpression(scheduledTime, daysOfWeek);
        model.addAttribute("successMessage", "Configuración guardada exitosamente.");
        System.out.println("ASDAFGDSF");
        return "redirect:/admin/accesspanel"; // Redirige al panel de acceso
    }
}
    