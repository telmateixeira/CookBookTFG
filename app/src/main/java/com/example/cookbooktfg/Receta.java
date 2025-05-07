package com.example.cookbooktfg;

import com.google.type.DateTime;

import java.util.List;

public class Receta {
    private String id;
    private String idCreador;
    private String descripcion;
    private String nombre;
    private String dificultad;
    private String duracion;
    private DateTime fechaCreacion;
    private String imagen;
    private List<String> ingredientes;
    private List<String> instrucciones;
    private boolean favorito;

    public Receta() {
        // Constructor vacío Firestore
    }

    public Receta(String id, String idCreador, String descripcion, String nombre, String dificultad, String duracion, DateTime fechaCreacion, String imagen, List<String> ingredientes, List<String> instrucciones, boolean favorito) {
        this.id = id;
        this.idCreador = idCreador;
        this.descripcion = descripcion;
        this.nombre = nombre;
        this.dificultad = dificultad;
        this.duracion = duracion;
        this.fechaCreacion = fechaCreacion;
        this.imagen = imagen;
        this.ingredientes = ingredientes;
        this.instrucciones = instrucciones;
        this.favorito = favorito;
    }

    // Getters y Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdCreador() {
        return idCreador;
    }

    public void setIdCreador(String idCreador) {
        this.idCreador = idCreador;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDificultad() {
        return dificultad;
    }

    public void setDificultad(String dificultad) {
        this.dificultad = dificultad;
    }

    public String getDuracion() {
        return duracion;
    }

    public void setDuracion(String duracion) {
        this.duracion = duracion;
    }

    public DateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(DateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public List<String> getIngredientes() {
        return ingredientes;
    }

    public void setIngredientes(List<String> ingredientes) {
        this.ingredientes = ingredientes;
    }

    public List<String> getInstrucciones() {
        return instrucciones;
    }

    public void setInstrucciones(List<String> instrucciones) {
        this.instrucciones = instrucciones;
    }

    public boolean isFavorito() {
        return favorito;
    }

    public void setFavorito(boolean favorito) {
        this.favorito = favorito;
    }
}
