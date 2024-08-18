package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.FirstBootStatus;

@Controller
public class HomeController {
    
     @Autowired
    private FirstBootStatus firstBootStatus; // Inyecta FirstBootStatus


    @GetMapping("/")
    public String home(Model model) {
        if (!firstBootStatus.isFirstBootCompleted()) {
            return "redirect:/firstboot"; // Redirige al proceso de firstboot si no se ha completado
        }else{
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String role = authentication.getAuthorities().iterator().next().getAuthority();
            if (role.equals("ROLE_ADMIN")) {
                return "redirect:/admin"; // Redirige al home del admin
            } else if (role.equals("ROLE_USER")) {
                return "redirect:/user"; // Redirige al home del usuario
            } else {
                return "redirect:/firstboot"; // Redirige a la página de login si no hay rol
            }
        }
    }

}
