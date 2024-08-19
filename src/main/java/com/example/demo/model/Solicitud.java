package com.example.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String radicado;

    private String estado; // Puede ser "PENDIENTE", "APROBADO", "RECHAZADO"

    // Constructores
    public Solicitud() {}

    public Solicitud(String radicado, String estado) {
        this.radicado = radicado;
        this.estado = estado;
    }

    // Getters y setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRadicado() {
        return radicado;
    }

    public void setRadicado(String radicado) {
        this.radicado = radicado;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
