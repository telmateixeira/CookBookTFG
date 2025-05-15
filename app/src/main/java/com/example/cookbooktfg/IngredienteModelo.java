package com.example.cookbooktfg;

public class IngredienteModelo {
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
    public String getTipo() { return tipo; }
    public String getNombre() { return nombre; }
    public String getCantidad() { return cantidad; }
}
