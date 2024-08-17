package com.example.demo.controller;

import com.example.demo.service.BddEditor;
import com.example.demo.service.WebScrapper;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final BddEditor bddEditor = new BddEditor();
    private final WebScrapper webScrapper = new WebScrapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public String adminHome(Model model) {
        // Display the adminHome view for administrators
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
                    result = "Número de radicado no proporcionado!";
                }
            } catch (Exception e) {
                result = "Error al ejecutar Acción 2: " + e.getMessage();
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
    public String createUser(@RequestParam String username, @RequestParam String password, @RequestParam String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // Encode the password
        System.err.println(role);
        // Find the role
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
        user.setPassword(passwordEncoder.encode(newPassword)); // Encode the new password
        userRepository.save(user);
        return "redirect:/admin/accesspanel";
    }
}
