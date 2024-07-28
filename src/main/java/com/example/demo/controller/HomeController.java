package com.example.demo.controller;

import com.example.demo.service.BddEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    private final BddEditor bddEditor = new BddEditor();

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @PostMapping("/execute")
    public String executeAction(@RequestParam(name = "action", required = false) String action, Model model) {
        String result;

        if ("action1".equals(action)) {
            try {
                bddEditor.agregarRadicado();
                result = "Acción 1 ejecutada con éxito!";
            } catch (Exception e) {
                result = "Error al ejecutar Acción 1: " + e.getMessage();
            }
        } else if ("action2".equals(action)) {
            result = "Acción 2 ejecutada!";
        } else {
            result = "Acción desconocida!";
        }

        model.addAttribute("result", result);
        return "home";
    }
}
