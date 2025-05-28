package com.example.cookbooktfg;

/**
 * Clase modelo que representa un ingrediente dentro de una receta.
 * Este modelo se utiliza para almacenar datos tanto en la app como en Firebase Firestore.
 *
 *  Autor: Telma Teixeira
 *  Proyecto: CookbookTFG
 */
public class IngredienteModelo {
    private String id;
    private String tipo;
    private String nombre;
    private String cantidad;

    /**
     * Constructor vacío requerido por Firestore para deserializar objetos.
     */
    public IngredienteModelo() {}

    /**
     * Constructor que inicializa un ingrediente con tipo, nombre y cantidad.
     *
     * @param tipo     Tipo de ingrediente.
     * @param nombre   Nombre del ingrediente.
     * @param cantidad Cantidad del ingrediente.
     */
    public IngredienteModelo(String tipo, String nombre, String cantidad) {
        this.tipo = tipo;
        this.nombre = nombre;
        this.cantidad = cantidad;
    }

    /**
     * Devuelve una representación del ingrediente en formato de texto,
     * útil para mostrarlo en chips u otras vistas resumidas.
     *
     * @return Texto con formato: "tipo nombre: cantidad".
     */
    public String getFormatoChip() {
        return tipo + " " + nombre + ": " + cantidad;
    }

    /**
     * Getters y setters
     */
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
