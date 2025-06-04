package com.example.cookbooktfg.repositorio;

import android.util.Log;

import com.example.cookbooktfg.modelos.Receta;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
/**
 * Clase de acceso a datos para las recetas en Firestore.
 * Proporciona métodos estáticos para obtener recetas generales,
 * recetas por lista de IDs y recetas favoritas del usuario actual.7
 *
 * Autor: Telma Teixeira
 * Proyecto: CookbookTFG
 */
public class RecetaRepositorio {
    /**
     * Obtiene todas las recetas de la colección "recetas" en Firestore.
     * Además, marca las recetas que son favoritas del usuario logueado.
     *
     * @param callback Función que recibe la lista de recetas obtenidas.
     *                 Puede ser una lista vacía si ocurre algún error.
     */

    public static void obtenerTodasLasRecetas(Consumer<List<Receta>> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("recetas").get().addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Receta> recetas = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Receta receta = doc.toObject(Receta.class);
                        receta.setId(doc.getId());
                        recetas.add(receta);
                    }
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) {
                        callback.accept(recetas);
                        return;
                    }
                    db.collection("usuarios").document(user.getUid()).get().addOnSuccessListener(userDoc -> {
                                List<DocumentReference> favoritas = (List<DocumentReference>) userDoc.get("favoritos");
                                Set<String> favoritasIds = new HashSet<>();
                                if (favoritas != null) {
                                    for (DocumentReference ref : favoritas) {
                                        favoritasIds.add(ref.getId());
                                    }
                                }
                                for (Receta receta : recetas) {
                                    receta.setFavorito(favoritasIds.contains(receta.getId()));
                                }
                                callback.accept(recetas);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Firestore", "Error al obtener usuario", e);
                                callback.accept(recetas);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error al obtener recetas", e);
                    callback.accept(new ArrayList<>());
                });
    }
    /**
     * Obtiene una lista de recetas cuyo ID esté en la lista proporcionada.
     *
     * @param recetaIds Lista con los IDs de las recetas a obtener.
     * @param callback Función que recibe la lista de recetas encontradas.
     *                 Devuelve lista vacía si recetaIds está vacía o si hay errores.
     */
    public static void obtenerRecetasPorIds(List<String> recetaIds, Consumer<List<Receta>> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<Receta> recetas = new ArrayList<>();

        if (recetaIds.isEmpty()) {
            callback.accept(recetas);
            return;
        }

        AtomicInteger contador = new AtomicInteger(0);
        for (String id : recetaIds) {
            db.collection("recetas").document(id).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Receta receta = doc.toObject(Receta.class);
                            if (receta != null) {
                                receta.setId(doc.getId());
                                recetas.add(receta);
                            }
                        }
                        // Cuando se hayan procesado todas las recetas, invocar callback
                        if (contador.incrementAndGet() == recetaIds.size()) {
                            callback.accept(recetas);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("RecetaRepositorio", "Error al obtener receta con ID: " + id, e);
                        if (contador.incrementAndGet() == recetaIds.size()) {
                            callback.accept(recetas); // Devuelve lo que tengas aunque haya fallos
                        }
                    });
        }
    }

    /**
     * Obtiene todas las recetas favoritas del usuario actualmente logueado.
     *
     * @param callback Función que recibe la lista de recetas favoritas.
     *                 Devuelve lista vacía si no hay usuario logueado o si hay errores.
     */
    public static void obtenerRecetasFavoritas(Consumer<List<Receta>> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.accept(new ArrayList<>());
            return;
        }

        FirebaseFirestore.getInstance().collection("usuarios")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<DocumentReference> favoritasRefs = (List<DocumentReference>) documentSnapshot.get("favoritos");
                    if (favoritasRefs == null || favoritasRefs.isEmpty()) {
                        callback.accept(new ArrayList<>());
                        return;
                    }

                    List<Receta> recetas = new ArrayList<>();
                    AtomicInteger contador = new AtomicInteger(0);

                    for (DocumentReference ref : favoritasRefs) {
                        ref.get().addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                Receta receta = doc.toObject(Receta.class);
                                if (receta != null) {
                                    receta.setId(doc.getId());
                                    receta.setFavorito(true);
                                    if (receta.getImagenes() == null) receta.setImagenes(new ArrayList<>());
                                    if (receta.getIngredientes() == null) receta.setIngredientes(new ArrayList<>());
                                    if (receta.getInstrucciones() == null) receta.setInstrucciones(new ArrayList<>());
                                    recetas.add(receta);
                                    Log.d("Favoritos", "Receta cargada de Firestore: nombre=" + receta.getNombre() + ", creadorId=" + receta.getCreadorId());
                                }
                            }

                            if (contador.incrementAndGet() == favoritasRefs.size()) {
                                Log.d("Favoritos", "Recetas favoritos cargadas: " + recetas.size());
                                callback.accept(recetas);
                            }
                        }).addOnFailureListener(e -> {
                            Log.e("Favoritos", "Error al obtener receta favorita", e);
                            if (contador.incrementAndGet() == favoritasRefs.size()) {
                                callback.accept(recetas); // aún si alguna falla
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Favoritos", "Error al obtener referencias de favoritos", e);
                    callback.accept(new ArrayList<>());
                });
    }
}
