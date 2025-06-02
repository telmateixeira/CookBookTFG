package com.example.cookbooktfg.modelos;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Clase que representa una receta de cocina en la aplicación.
 * Contiene toda la información necesaria para mostrar, almacenar y convertir recetas.
 *
 *  Autor: Telma Teixeira
 *  Proyecto: CookbookTFG
 */
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
    private transient List<String> nombresIngredientes; // transient para que no se guarde en Firestore
    private List<DocumentReference> instrucciones;
    private boolean favorito;

    private String fuente; // Fuente de la receta: "usuario" o "spoonacular"
    private int idSpoonacular; // ID original de la receta si proviene de Spoonacular

    /**
     * Constructor vacío requerido por Firestore para la deserialización.
     */
    public Receta() {}

    /**
     * Constructor con parámetros para inicializar una receta completa.
     */
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

    // --- Getters y Setters ---
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
    /**
     * Devuelve una lista de los nombres de los ingredientes.
     * Si aún no se ha inicializado, se crea una lista vacía con nombres vacíos por defecto.
     */
    public List<String> getNombresIngredientes() {
        if (nombresIngredientes == null) {
            nombresIngredientes = new ArrayList<>();
            if (ingredientes != null) {
                for (DocumentReference ref : ingredientes) {
                    // Esto debería llenarse cuando se cargan las recetas
                    nombresIngredientes.add("");
                }
            }
        }
        return nombresIngredientes;
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

    public void setNombresIngredientes(List<String> nombresIngredientes) {
        this.nombresIngredientes = nombresIngredientes;
    }

    public String getFuente() {
        return fuente;
    }

    public void setFuente(String fuente) {
        this.fuente = fuente;
    }

    public int getIdSpoonacular() {
        return idSpoonacular;
    }

    public void setIdSpoonacular(int idSpoonacular) {
        this.idSpoonacular = idSpoonacular;
    }

    /**
     * Calcula la dificultad de una receta basándose en la duración estimada.
     *
     * @param duracion Texto que representa la duración (puede contener letras)
     * @return "Fácil", "Media" o "Difícil"
     */
    public static String calcularDificultad(String duracion) {
        try {
            int minutos = Integer.parseInt(duracion.replaceAll("[^0-9]", ""));
            if (minutos < 30) return "Fácil";
            if (minutos < 60) return "Media";
            return "Difícil";
        } catch (NumberFormatException e) {
            return "Media";
        }
    }
}
