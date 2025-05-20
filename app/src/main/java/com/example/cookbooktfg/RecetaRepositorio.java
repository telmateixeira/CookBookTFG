package com.example.cookbooktfg;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class RecetaRepositorio {

    public static void obtenerTodasLasRecetas(Consumer<List<Receta>> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("recetas")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Receta> recetas = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Receta receta = doc.toObject(Receta.class);
                        receta.setId(doc.getId());
                        recetas.add(receta);
                    }

                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) {
                        callback.accept(recetas); // No logueado, sin favoritos
                        return;
                    }

                    db.collection("usuarios")
                            .document(user.getUid())
                            .get()
                            .addOnSuccessListener(userDoc -> {
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

    public interface CallbackRecetas {
        void onRecetasCargadas(List<Receta> recetas);
    }

    public static void obtenerRecetasPorIds(List<String> ids, CallbackRecetas callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (ids.isEmpty()) {
            callback.onRecetasCargadas(new ArrayList<>());
            return;
        }

        db.collection("recetas")
                .whereIn(FieldPath.documentId(), ids)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Receta> recetas = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Receta receta = doc.toObject(Receta.class);
                        if (receta != null) {
                            receta.setId(doc.getId());
                            recetas.add(receta);
                        }
                    }
                    callback.onRecetasCargadas(recetas);
                })
                .addOnFailureListener(e -> {
                    Log.e("RecetaRepositorio", "Error al cargar recetas por IDs", e);
                    callback.onRecetasCargadas(new ArrayList<>());
                });
    }



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
