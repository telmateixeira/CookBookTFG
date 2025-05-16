package com.example.cookbooktfg;

public class IngredienteModelo {
    private String id;
    private String tipo;
    private String nombre;
    private String cantidad;

    // Constructor
    public IngredienteModelo(String tipo, String nombre, String cantidad) {
        this.tipo = tipo;
        this.nombre = nombre;
        this.cantidad = cantidad;
    }

    // Formato para mostrar en chips
    public String getFormatoChip() {
        return tipo + " " + nombre + ": " + cantidad;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getTipo() { return tipo; }
    public String getNombre() { return nombre; }
    public String getCantidad() { return cantidad; }

    public void setId(String id) {
        this.id = id;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public void setCantidad(String cantidad) {
        this.cantidad = cantidad;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
