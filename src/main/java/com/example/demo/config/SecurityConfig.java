package com.example.demo.config;

import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.CustomUserDetailsService;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/login").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/firstboot/**").hasRole("ADMIN")
                .requestMatchers("/h2-console/**").hasRole("ADMIN") // Permite el acceso a la consola H2
                .requestMatchers("/").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .rememberMe(rememberMe -> rememberMe
                .key("uniqueAndSecretKey") // Puedes personalizar esta clave
                .tokenValiditySeconds(86400) // Duración del token en segundos (1 día en este ejemplo)
                .userDetailsService(userDetailsService)
            )
            .logout(logout -> logout.permitAll())
            
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**") // Desactiva CSRF para la consola H2
            )
            .headers(headers -> headers
            .httpStrictTransportSecurity(hsts -> hsts.disable()) // Desactiva HSTS si es necesario
            .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline';")) // Ajusta la política de seguridad de contenido
            .frameOptions(frameOptions -> frameOptions.sameOrigin()) // Permite frames solo desde el mismo origen
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CommandLineRunner dataInitializer(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            long userCount = userRepository.count();
            if (userCount == 0) {
                // Imprimir todos los usuarios
                System.out.println("No hay usuarios registrados.");
                
                // Crear roles
                Role adminRole = new Role();
                adminRole.setName("ADMIN"); // Define el rol sin el prefijo ROLE_
                roleRepository.save(adminRole);

                Role userRole = new Role();
                userRole.setName("USER"); // Define el rol sin el prefijo ROLE_
                roleRepository.save(userRole);

                // Crear usuario admin
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123")); // Contraseña segura

                Set<Role> roles = new HashSet<>();
                roles.add(adminRole); // Asignar el rol admin sin el prefijo ROLE_
                admin.setRoles(roles);

                userRepository.save(admin);

                System.out.println("Usuario admin creado. Puedes iniciar sesión con el usuario admin.");
            } else {
                System.out.println("Usuarios en la base de datos:");
                for (User user : userRepository.findAll()) {
                    System.out.println("Usuario: " + user.getUsername());
                }
            }
        };
    }
}
