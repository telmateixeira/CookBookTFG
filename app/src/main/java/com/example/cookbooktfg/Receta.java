package com.example.cookbooktfg;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Exclude;

import java.util.Date;
import java.util.List;

public class Receta {
    @Exclude
    private String id;
    private String creadorId;
    private String descripcion;
    private String nombre;
    private String dificultad;
    private String duracion;
    private Date fechaCreacion;
    private List<String> imagenes;
    private List<DocumentReference> ingredientes;
    private List<DocumentReference> instrucciones;
    private boolean favorito;

    public Receta() {
        // Constructor vacío Firestore
    }

    public Receta(String id, String creadorId, String nombre, String descripcion, String dificultad, String duracion, List<String> imagenes, Date fechaCreacion, List<DocumentReference> ingredientes, List<DocumentReference> instrucciones, boolean favorito) {
        this.id = id;
        this.creadorId = creadorId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.dificultad = dificultad;
        this.duracion = duracion;
        this.imagenes = imagenes;
        this.fechaCreacion = fechaCreacion;
        this.ingredientes = ingredientes;
        this.instrucciones = instrucciones;
        this.favorito = favorito;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreadorId() {
        return creadorId;
    }

    public void setCreadorId(String creadorId) {
        this.creadorId = creadorId;
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

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public List<String> getImagenes() {
        return imagenes;
    }

    public void setImagenes(List<String> imagenes) {
        this.imagenes = imagenes;
    }

    public List<DocumentReference> getIngredientes() {
        return ingredientes;
    }

    public void setIngredientes(List<DocumentReference> ingredientes) {
        this.ingredientes = ingredientes;
    }

    public List<DocumentReference> getInstrucciones() {
        return instrucciones;
    }

    public void setInstrucciones(List<DocumentReference> instrucciones) {
        this.instrucciones = instrucciones;
    }

    public boolean isFavorito() {
        return favorito;
    }

    public void setFavorito(boolean favorito) {
        this.favorito = favorito;
    }
}
