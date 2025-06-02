package com.example.cookbooktfg.utils;

import android.util.Log;

import com.example.cookbooktfg.BuildConfig;
import com.example.cookbooktfg.modelos.IngredienteModelo;
import com.example.cookbooktfg.modelos.InstruccionModelo;
import com.example.cookbooktfg.modelos.Receta;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Clase responsable de obtener recetas desde la API Spoonacular,
 * procesarlas y guardarlas en Firestore, evitando duplicados.
 * También maneja la creación de subcolecciones de ingredientes e instrucciones.
 *
 *  Autor: Telma Teixeira
 *  Proyecto: CookbookTFG
 */
public class RecetasGenerator {
    private static final String TAG = "RecetasGenerator";
    private final FirebaseFirestore db;
    private final SpoonacularService api;
    private final String userId;

    /**
     * Constructor que inicializa Firestore, API y usuario.
     * @param userId ID del usuario que realizará la importación de recetas.
     */
    public RecetasGenerator(String userId) {
        this.db = FirebaseFirestore.getInstance();
        this.api = SpoonacularClient.getInstance();
        this.userId = userId;
    }

    /**
     * Interface para callbacks de la generación de recetas.
     * Permite comunicar éxito, existencia previa o error.
     */
    public interface RecetasCallback {
        void onSuccess(int recetasGeneradas);
        void onRecetasExisten();
        void onError(String mensaje);
    }

    /**
     * Verifica si ya existen recetas importadas desde Spoonacular,
     * y en caso contrario lanza la generación/importación.
     * @param cantidad Número de recetas a generar si no existen.
     * @param callback Interfaz para recibir resultados o errores.
     */
    public void generarRecetasSiEsNecesario(int cantidad, RecetasCallback callback) {
        db.collection("recetas")
                .whereEqualTo("fuente", "spoonacular")
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            callback.onRecetasExisten();
                        } else {
                            generarRecetasDesdeAPI(cantidad, callback);
                        }
                    } else {
                        callback.onError("Error verificando recetas existentes");
                    }
                });
    }

    /**
     * Realiza la llamada a la API Spoonacular para obtener recetas aleatorias.
     * @param cantidad Número de recetas a solicitar.
     * @param callback Interfaz para recibir resultados o errores.
     */
    private void generarRecetasDesdeAPI(int cantidad, RecetasCallback callback) {
        api.getRandomRecipes(BuildConfig.SPOONACULAR_API_KEY, cantidad)
                .enqueue(new Callback<SpoonacularReceta.SpoonacularResponse>() {
                    @Override
                    public void onResponse(Call<SpoonacularReceta.SpoonacularResponse> call, Response<SpoonacularReceta.SpoonacularResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            procesarYGuardarRecetas(response.body().getRecipes(), callback);
                        } else {
                            callback.onError("Error en la API: " + response.message());
                        }
                    }

                    @Override
                    public void onFailure(Call<SpoonacularReceta.SpoonacularResponse> call, Throwable t) {
                        callback.onError("Fallo de conexión: " + t.getMessage());
                    }
                });
    }

    /**
     * Procesa la lista de recetas obtenidas desde la API,
     * convierte a objetos Receta y verifica duplicados antes de guardar.
     * @param apiRecetas Lista de recetas desde Spoonacular.
     * @param callback Interfaz para notificar resultados.
     */
    private void procesarYGuardarRecetas(List<SpoonacularReceta.SpoonacularRecipe> apiRecetas, RecetasCallback callback) {
        if (apiRecetas == null || apiRecetas.isEmpty()) {
            callback.onError("No se recibieron recetas");
            return;
        }

        AtomicInteger exitosas = new AtomicInteger(0);
        final int totalRecetas = apiRecetas.size();

        for (SpoonacularReceta.SpoonacularRecipe apiReceta : apiRecetas) {
            Receta nuevaReceta = convertirReceta(apiReceta);

            db.collection("recetas")
                    .whereEqualTo("idSpoonacular", apiReceta.getId())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (task.getResult().isEmpty()) {
                                guardarRecetaConSubcolecciones(nuevaReceta, apiReceta, exitosas, totalRecetas, callback);
                            } else {
                                Log.d(TAG, "Receta duplicada omitida: " + apiReceta.getId());
                                if (exitosas.incrementAndGet() == totalRecetas) {
                                    callback.onSuccess(totalRecetas);
                                }
                            }
                        }
                    });
        }
    }

    /**
     * Convierte un objeto SpoonacularRecipe a un objeto Receta personalizado,
     * limpiando campos y preparando estructura para guardar en Firestore.
     * @param apiReceta Receta obtenida desde Spoonacular API.
     * @return Receta objeto para Firestore.
     */
    private Receta convertirReceta(SpoonacularReceta.SpoonacularRecipe apiReceta) {
        Receta receta = new Receta();
        receta.setIdSpoonacular(apiReceta.getId());
        receta.setFuente("spoonacular");
        receta.setNombre(apiReceta.getTitle());
        receta.setDescripcion(apiReceta.getSummary().replaceAll("<[^>]*>", ""));
        receta.setCreadorId("spoonacular");
        receta.setDuracion(String.valueOf(apiReceta.getReadyInMinutes()));
        receta.setDificultad(Receta.calcularDificultad(String.valueOf(apiReceta.getReadyInMinutes())));
        receta.setFavorito(false);
        receta.setFechaCreacion(new Date());

        List<String> imagenes = new ArrayList<>();
        if (apiReceta.getImage() != null) {
            String imageUrl = apiReceta.getImage();
            if (imageUrl != null && imageUrl.endsWith(".")) {
                imageUrl = imageUrl.substring(0, imageUrl.length() - 1);
                imagenes.add(imageUrl);
            }
        }
        receta.setImagenes(imagenes);

        receta.setIngredientes(new ArrayList<>());
        receta.setInstrucciones(new ArrayList<>());

        return receta;
    }

    /**
     * Guarda una receta en Firestore y luego guarda sus ingredientes e instrucciones
     * en subcolecciones relacionadas, finalmente actualiza la receta con las referencias.
     * @param receta Objeto Receta a guardar.
     * @param apiReceta Datos originales desde Spoonacular.
     * @param exitosas Contador atómico para recetas guardadas exitosamente.
     * @param total Total de recetas a procesar.
     * @param callback Callback para notificar resultados.
     */
    private void guardarRecetaConSubcolecciones(Receta receta, SpoonacularReceta.SpoonacularRecipe apiReceta, AtomicInteger exitosas, int total, RecetasCallback callback) {
        receta.setCreadorId(userId);
        // Guardamos la receta primero sin referencias de ingredientes ni instrucciones
        db.collection("recetas")
                .add(receta)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Receta guardada: " + documentReference.getId());

                    // Guardamos ingredientes e instrucciones y al final actualizamos la receta con las referencias
                    guardarIngredientesEInstruccionesYActualizar(documentReference, apiReceta, exitosas, total, callback);

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error guardando receta", e);
                    if (exitosas.incrementAndGet() == total) {
                        callback.onSuccess(exitosas.get());
                    }
                });
    }
    /**
     * Guarda ingredientes e instrucciones como documentos independientes en Firestore,
     * luego actualiza la receta original con referencias a estos documentos.
     * Maneja operaciones asíncronas y controla el flujo mediante contadores.
     * @param recetaRef Referencia al documento de receta guardado.
     * @param apiReceta Receta original desde Spoonacular.
     * @param exitosas Contador atómico de recetas exitosas.
     * @param total Total de recetas a procesar.
     * @param callback Callback para notificar resultados.
     */
    private void guardarIngredientesEInstruccionesYActualizar(DocumentReference recetaRef, SpoonacularReceta.SpoonacularRecipe apiReceta,
                                                              AtomicInteger exitosas, int total, RecetasCallback callback) {
        List<DocumentReference> referenciasIngredientes = new ArrayList<>();
        List<DocumentReference> referenciasInstrucciones = new ArrayList<>();

        List<SpoonacularReceta.SpoonacularExtendedIngredient> ingredientes = apiReceta.getExtendedIngredients();
        List<SpoonacularReceta.SpoonacularAnalyzedInstruction> instrucciones = apiReceta.getAnalyzedInstructions();

        // Contadores para saber cuándo terminar las operaciones asincrónicas
        AtomicInteger pendientes = new AtomicInteger(0);

        // Guardar ingredientes
        if (ingredientes != null && !ingredientes.isEmpty()) {
            pendientes.addAndGet(ingredientes.size());
            for (SpoonacularReceta.SpoonacularExtendedIngredient ing : ingredientes) {
                IngredienteModelo ingrediente = new IngredienteModelo();
                ingrediente.setNombre(ing.getName());
                ingrediente.setTipo(ing.getAisle());
                ingrediente.setCantidad(String.valueOf(ing.getAmount()));

                db.collection("ingredientes")
                        .add(ingrediente)
                        .addOnSuccessListener(docRef -> {
                            referenciasIngredientes.add(docRef);
                            if (pendientes.decrementAndGet() == 0) {
                                // Cuando termine de guardar ingredientes e instrucciones, actualizar receta
                                actualizarRecetaConReferencias(recetaRef, referenciasIngredientes, referenciasInstrucciones, exitosas, total, callback);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error añadiendo ingrediente", e);
                            if (pendientes.decrementAndGet() == 0) {
                                actualizarRecetaConReferencias(recetaRef, referenciasIngredientes, referenciasInstrucciones, exitosas, total, callback);
                            }
                        });
            }
        }

        // Guardar instrucciones
        if (instrucciones != null && !instrucciones.isEmpty()) {
            for (SpoonacularReceta.SpoonacularAnalyzedInstruction instr : instrucciones) {
                if (instr.getSteps() == null) continue;
                pendientes.addAndGet(instr.getSteps().size());

                for (SpoonacularReceta.SpoonacularStep paso : instr.getSteps()) {
                    InstruccionModelo instruccion = new InstruccionModelo();
                    instruccion.setOrden(paso.getNumber());
                    instruccion.setPaso(paso.getStep());

                    db.collection("instrucciones")
                            .add(instruccion)
                            .addOnSuccessListener(docRef -> {
                                referenciasInstrucciones.add(docRef);
                                if (pendientes.decrementAndGet() == 0) {
                                    actualizarRecetaConReferencias(recetaRef, referenciasIngredientes, referenciasInstrucciones, exitosas, total, callback);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error añadiendo paso", e);
                                if (pendientes.decrementAndGet() == 0) {
                                    actualizarRecetaConReferencias(recetaRef, referenciasIngredientes, referenciasInstrucciones, exitosas, total, callback);
                                }
                            });
                }
            }
        }

        // Si no hay ingredientes ni instrucciones, actualizar directamente
        if ((ingredientes == null || ingredientes.isEmpty()) && (instrucciones == null || instrucciones.isEmpty())) {
            actualizarRecetaConReferencias(recetaRef, referenciasIngredientes, referenciasInstrucciones, exitosas, total, callback);
        }
    }
    /**
     * Actualiza el documento de la receta con las referencias a los documentos
     * de ingredientes e instrucciones recién guardados.
     * @param recetaRef Referencia al documento receta.
     * @param ingredientesRefs Lista de referencias a documentos de ingredientes.
     * @param instruccionesRefs Lista de referencias a documentos de instrucciones.
     * @param exitosas Contador atómico de recetas exitosas.
     * @param total Total de recetas a procesar.
     * @param callback Callback para notificar resultados.
     */
    private void actualizarRecetaConReferencias(DocumentReference recetaRef, List<DocumentReference> ingredientesRefs,
                                                List<DocumentReference> instruccionesRefs, AtomicInteger exitosas,
                                                int total, RecetasCallback callback) {
        recetaRef.update(
                "ingredientes", ingredientesRefs,
                "instrucciones", instruccionesRefs
        ).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Receta actualizada con referencias a ingredientes e instrucciones");
            if (exitosas.incrementAndGet() == total) {
                callback.onSuccess(total);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error actualizando receta con referencias", e);
            if (exitosas.incrementAndGet() == total) {
                callback.onSuccess(exitosas.get());
            }
        });
    }

}
