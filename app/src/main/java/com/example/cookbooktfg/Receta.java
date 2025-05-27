package com.example.cookbooktfg;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
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
    private transient List<String> nombresIngredientes; // transient para que no se guarde en Firestore
    private List<DocumentReference> instrucciones;
    private boolean favorito;
    private String fuente; // "spoonacular" o "usuario"
    private int idSpoonacular; // ID original de Spoonacular


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
    public List<String> getNombresIngredientes() {
        if (nombresIngredientes == null) {
            nombresIngredientes = new ArrayList<>();
            if (ingredientes != null) {
                for (DocumentReference ref : ingredientes) {
                    // Esto debería llenarse cuando cargas las recetas
                    nombresIngredientes.add(""); // O el nombre real si lo tienes
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
    // Método estático para convertir SpoonacularRecipe a Receta
    public static Receta fromSpoonacular(SpoonacularReceta.SpoonacularRecipe spoonRecipe, String creadorId) {
        Receta receta = new Receta();

        receta.setIdSpoonacular(spoonRecipe.getId());
        receta.setFuente("spoonacular");
        receta.setNombre(spoonRecipe.getTitle());
        receta.setDescripcion(spoonRecipe.getSummary() != null ? spoonRecipe.getSummary().replaceAll("<[^>]*>", "") : "");
        receta.setCreadorId(creadorId);
        receta.setDuracion(String.valueOf(spoonRecipe.getReadyInMinutes()));
        receta.setDificultad(calcularDificultad(receta.getDuracion()));
        receta.setFavorito(false);
        receta.setFechaCreacion(new Date());

        List<String> imagenes = new ArrayList<>();
        if (spoonRecipe.getImage() != null) {
            imagenes.add(spoonRecipe.getImage());
        }
        receta.setImagenes(imagenes);

        // Ingredientes e instrucciones se asignan luego porque requieren acceso a Firestore
        receta.setIngredientes(new ArrayList<>());
        receta.setInstrucciones(new ArrayList<>());

        return receta;
    }

    // Método para calcular dificultad según duración en minutos
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
