package com.example.demo.controller;

import com.example.demo.service.BddEditor;
import com.example.demo.service.WebScrapper;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/user")
public class UserController {
    @Autowired
    private BddEditor bddEditor;

    @Autowired
    private WebScrapper webScrapper;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public String userHome(Model model) {
        // Display the userHome view for normal users
        model.addAttribute("users", userRepository.findAll());
        return "userHome"; // User-specific view
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
        return "userHome"; // Return to userHome view
    }
    @GetMapping("/bdd")
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

        return "bdd";
    }
}
