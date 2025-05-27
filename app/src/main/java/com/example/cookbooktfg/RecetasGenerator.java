package com.example.cookbooktfg;

import android.util.Log;

import com.example.cookbooktfg.BuildConfig;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class RecetasGenerator {
    private static final String TAG = "RecetasGenerator";
    private final FirebaseFirestore db;
    private final SpoonacularService api;
    private final String userId;

    public RecetasGenerator(String userId) {
        this.db = FirebaseFirestore.getInstance();
        this.api = SpoonacularClient.getInstance();
        this.userId = userId;
    }

    public interface RecetasCallback {
        void onSuccess(int recetasGeneradas);
        void onRecetasExisten();
        void onError(String mensaje);
    }

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
            imagenes.add(apiReceta.getImage());
        }
        receta.setImagenes(imagenes);

        receta.setIngredientes(new ArrayList<>());
        receta.setInstrucciones(new ArrayList<>());

        return receta;
    }

    private void guardarRecetaConSubcolecciones(Receta receta, SpoonacularReceta.SpoonacularRecipe apiReceta, AtomicInteger exitosas, int total, RecetasCallback callback) {
        // Ajustamos el creadorId con el userId
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
