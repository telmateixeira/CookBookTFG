package com.example.cookbooktfg.utils;
import com.example.cookbooktfg.modelos.IngredienteModelo;
import com.example.cookbooktfg.modelos.InstruccionModelo;
import com.example.cookbooktfg.modelos.Receta;
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
     * Incluye detalles como título, imagen, ingredientes e instrucciones.
     */
    public static class SpoonacularRecipe {
        private int id;
        private String title;
        private String image;
        private String summary;
        private String readyInMinutes;
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
}