package com.example.demo.controller;

import com.example.demo.service.BddEditor;
import com.example.demo.service.WebScrapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
public class HomeController {

    private final BddEditor bddEditor = new BddEditor();
    private final WebScrapper webScrapper = new WebScrapper();

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @PostMapping("/execute")
    public String executeAction(@RequestParam(name = "action", required = false) String action,
                                @RequestParam(name = "radNumber", required = false) String radNumber,
                                Model model) {
        String result;

        if ("action1".equals(action)) {
            try {
                if (radNumber != null && !radNumber.isEmpty()) {
                    radNumber = radNumber.toString();
                    bddEditor.agregarRadicado(radNumber);
                    result = "Acción 1 ejecutada con éxito!";
                } else {
                    result = "Número de radicado no proporcionado!";
                }
            } catch (Exception e) {
                result = "Error al ejecutar Acción 1: " + e.getMessage();
            }
        } else if ("action2".equals(action)) {
            try {
                if (radNumber != null && !radNumber.isEmpty()) {
                    radNumber = radNumber.toString();
                    Map<String, Object> process = webScrapper.queryProcess(radNumber);
                    model.addAttribute("process", process); // Agrega los datos al modelo para mostrarlos
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
        return "home";
    }
}
