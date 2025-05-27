package com.example.cookbooktfg;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SpoonacularReceta {

    // Modelo para la respuesta JSON de Spoonacular (nombre original: "recipes")
    public static class SpoonacularResponse {
        private List<SpoonacularRecipe> recipes; // ¡Ojo al nombre! Debe coincidir con la API

        public List<SpoonacularRecipe> getRecipes() {
            return recipes;
        }
    }

    // Modelo para receta (campos exactos de Spoonacular)
    public static class SpoonacularRecipe {
        private int id;
        private String title; // "title" en la API, no "titulo"
        private String image;
        private String summary; // "summary" contiene HTML
        private String readyInMinutes;
        private List<String> dishTypes;
        private List<SpoonacularExtendedIngredient> extendedIngredients; // Nombre exacto de la API
        private List<SpoonacularAnalyzedInstruction> analyzedInstructions; // Nombre exacto

        // Getters
        public int getId() { return id; }
        public String getTitle() { return title; }
        public String getImage() { return image; }
        public String getSummary() { return summary; }
        public String getReadyInMinutes() { return readyInMinutes; }
        public List<SpoonacularExtendedIngredient> getExtendedIngredients() { return extendedIngredients; }
        public List<SpoonacularAnalyzedInstruction> getAnalyzedInstructions() { return analyzedInstructions; }
    }

    // Modelo para ingredientes extendidos (Spoonacular)
    public static class SpoonacularExtendedIngredient {
        private String name; // Nombre simple
        private String nameClean; // Nombre limpio (sin detalles)
        private String aisle; // tipo (ej: "Dairy")
        private double amount;
        private String unit;

        // Getters
        public String getName() { return name; }
        public String getNameClean() { return nameClean; }
        public String getAisle() { return aisle; } // Útil para el campo "tipo" en Firestore

        public double getAmount() {
            return amount;
        }

        public String getUnit() {
            return unit;
        }
    }

    // Modelo para instrucciones analizadas
    public static class SpoonacularAnalyzedInstruction {
        private String name; // Ej: "Instructions"
        private List<SpoonacularStep> steps;

        public List<SpoonacularStep> getSteps() { return steps; }
    }

    // Modelo para pasos
    public static class SpoonacularStep {
        private int number;
        private String step; // Texto del paso

        public String getStep() { return step; }
        public int getNumber() { return number; }
    }

    // Método de conversión a tu clase Receta (versión mejorada)
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


    public interface OnRecipeConvertedListener {
        void onRecipeReady(Receta receta);
    }

    private static String calculateDifficulty(int minutes) {
        return minutes < 30 ? "Fácil" : minutes < 60 ? "Media" : "Difícil";
    }
}