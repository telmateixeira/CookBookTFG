package com.example.cookbooktfg;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Clase que modela la estructura de datos recibida desde la API Spoonacular,
 * incluyendo clases internas que representan recetas, ingredientes e instrucciones.
 * Además, incluye un metodo para convertir objetos Spoonacular a la clase local Receta
 * y guardarlos en Firestore, creando referencias a ingredientes e instrucciones.
 */
public class SpoonacularReceta {

    /**
     * Modelo que representa la respuesta JSON general de la API Spoonacular
     * para la obtención de recetas aleatorias (campo "recipes").
     */
    public static class SpoonacularResponse {
        private List<SpoonacularRecipe> recipes;
        /**
         * Obtiene la lista de recetas.
         * @return Lista de objetos SpoonacularRecipe.
         */
        public List<SpoonacularRecipe> getRecipes() {
            return recipes;
        }
    }

    /**
     * Modelo que representa una receta según la estructura de Spoonacular.
     * Incluye detalles como título, imagen, resumen HTML, ingredientes e instrucciones.
     */
    public static class SpoonacularRecipe {
        private int id;
        private String title;
        private String image;
        private String summary;
        private String readyInMinutes;
        private List<String> dishTypes;
        private List<SpoonacularExtendedIngredient> extendedIngredients;
        private List<SpoonacularAnalyzedInstruction> analyzedInstructions;

        // Getters para acceder a los campos privados
        public int getId() { return id; }
        public String getTitle() { return title; }
        public String getImage() { return image; }
        public String getSummary() { return summary; }
        public String getReadyInMinutes() { return readyInMinutes; }
        public List<SpoonacularExtendedIngredient> getExtendedIngredients() { return extendedIngredients; }
        public List<SpoonacularAnalyzedInstruction> getAnalyzedInstructions() { return analyzedInstructions; }
    }

    /**
     * Modelo que representa un ingrediente extendido de Spoonacular,
     * con información como nombre, categoría (pasillo), cantidad y unidad.
     */
    public static class SpoonacularExtendedIngredient {
        private String name;
        private String nameClean;
        private String aisle;
        private double amount;
        private String unit;

        // Getters
        public String getName() { return name; }
        public String getNameClean() { return nameClean; }
        public String getAisle() { return aisle; } // Útil para el campo "tipo" en Firestore

        public double getAmount() {return amount;}

        public String getUnit() {return unit;}
    }

    /**
     * Modelo que representa las instrucciones analizadas de la receta.
     * Contiene un nombre descriptivo y una lista de pasos.
     */
    public static class SpoonacularAnalyzedInstruction {
        private String name;
        private List<SpoonacularStep> steps;
        /**
         * Obtiene la lista de pasos.
         * @return Lista de objetos SpoonacularStep.
         */
        public List<SpoonacularStep> getSteps() { return steps; }
    }

    /**
     * Modelo que representa un paso individual dentro de las instrucciones.
     * Contiene el número del paso y la descripción textual.
     */
    public static class SpoonacularStep {
        private int number;
        private String step;

        public String getStep() { return step; }
        public int getNumber() { return number; }
    }

    /**
     * Metodo estático que convierte un objeto SpoonacularRecipe a un objeto local Receta,
     * y guarda la receta junto con sus ingredientes e instrucciones en Firestore.
     *
     * @param apiRecipe Objeto SpoonacularRecipe recibido desde la API.
     * @param creadorId ID del usuario creador que añade la receta.
     * @param db        Instancia de FirebaseFirestore para operaciones de base de datos.
     * @param listener  Callback que se invoca cuando la receta ya está lista con todas sus referencias.
     */
    public static void convertToFirestoreRecipe(SpoonacularRecipe apiRecipe, String creadorId,
                                                FirebaseFirestore db, OnRecipeConvertedListener listener) {
        Receta receta = new Receta();
        receta.setNombre(apiRecipe.getTitle());
        receta.setDescripcion(apiRecipe.getSummary().replaceAll("<[^>]*>", ""));
        receta.setDuracion(apiRecipe.getReadyInMinutes());

        try {
            int minutos = Integer.parseInt(apiRecipe.getReadyInMinutes());
            receta.setDificultad(calculateDifficulty(minutos));
        } catch (NumberFormatException e) {
            receta.setDificultad("Media");
        }

        receta.setImagenes(List.of(apiRecipe.getImage()));
        receta.setCreadorId(creadorId);
        receta.setFechaCreacion(new Date());

        List<DocumentReference> referenciasIngredientes = new ArrayList<>();
        List<DocumentReference> referenciasInstrucciones = new ArrayList<>();

        // Guardar ingredientes en Firestore
        List<Task<DocumentReference>> tareasIngredientes = new ArrayList<>();
        for (SpoonacularExtendedIngredient ing : apiRecipe.getExtendedIngredients()) {
            String tipo = ing.getAisle() != null ? ing.getAisle() : "Otros";
            String nombre = ing.getNameClean() != null ? ing.getNameClean() : ing.getName();
            String cantidad = String.format("%.1f %s", ing.getAmount(), ing.getUnit()).trim();
            if (cantidad.endsWith(".0")) cantidad = cantidad.replace(".0", "");

            IngredienteModelo ingrediente = new IngredienteModelo(tipo, nombre, cantidad);
            tareasIngredientes.add(db.collection("ingredientes").add(ingrediente));
        }

        // Guardar instrucciones en Firestore
        List<Task<DocumentReference>> tareasInstrucciones = new ArrayList<>();
        if (apiRecipe.getAnalyzedInstructions() != null && !apiRecipe.getAnalyzedInstructions().isEmpty()) {
            for (SpoonacularStep paso : apiRecipe.getAnalyzedInstructions().get(0).getSteps()) {
                InstruccionModelo instruccion = new InstruccionModelo(paso.getNumber(), paso.getStep());
                tareasInstrucciones.add(db.collection("instrucciones").add(instruccion));
            }
        }

        // Esperar que se guarden los ingredientes
        Tasks.whenAllComplete(tareasIngredientes)
                .addOnSuccessListener(ingredientesTasks -> {
                    for (Task<?> t : ingredientesTasks) {
                        if (t.isSuccessful()) {
                            referenciasIngredientes.add(((Task<DocumentReference>) t).getResult());
                        }
                    }

                    receta.setIngredientes(referenciasIngredientes);

                    // Esperar que se guarden las instrucciones
                    Tasks.whenAllComplete(tareasInstrucciones)
                            .addOnSuccessListener(instruccionesTasks -> {
                                for (Task<?> t : instruccionesTasks) {
                                    if (t.isSuccessful()) {
                                        referenciasInstrucciones.add(((Task<DocumentReference>) t).getResult());
                                    }
                                }

                                receta.setInstrucciones(referenciasInstrucciones);
                                listener.onRecipeReady(receta); // <- receta lista con referencias
                            });
                });
    }

    /**
     * Interfaz callback para notificar cuando la receta ya está convertida y lista.
     */
    public interface OnRecipeConvertedListener {
        /**
         * Se llama cuando la receta ha sido convertida y guardada con éxito,
         * incluyendo las referencias a ingredientes e instrucciones en Firestore.
         *
         * @param receta Objeto Receta listo para usarse.
         */
        void onRecipeReady(Receta receta);
    }

    /**
     * Calcula una dificultad aproximada basada en la duración en minutos.
     *
     * @param minutes Tiempo de preparación en minutos.
     * @return String que representa la dificultad ("Fácil", "Media" o "Difícil").
     */
    private static String calculateDifficulty(int minutes) {
        return minutes < 30 ? "Fácil" : minutes < 60 ? "Media" : "Difícil";
    }
}