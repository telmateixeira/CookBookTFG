package com.example.cookbooktfg;

import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RecetaRepositorio {

    public static void obtenerTodasLasRecetas(Consumer<List<Receta>> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("recetas")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Receta> recetas = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Receta receta = new Receta();

                        receta.setId(doc.getId());
                        receta.setNombre(doc.getString("nombre"));
                        receta.setDescripcion(doc.getString("descripcion"));
                        receta.setImagen(doc.getString("imagen"));
                        receta.setDificultad(doc.getString("dificultad"));
                        receta.setDuracion(doc.getString("duracion"));
                        receta.setFavorito(Boolean.TRUE.equals(doc.getBoolean("favorito")));
                        receta.setCreadorId(doc.getString("creadorId"));

                        List<DocumentReference> ingredientes = (List<DocumentReference>) doc.get("ingredientes");
                        receta.setIngredientes(ingredientes != null ? ingredientes : new ArrayList<>());

                        List<DocumentReference> instrucciones = (List<DocumentReference>) doc.get("instrucciones");
                        receta.setInstrucciones(instrucciones != null ? instrucciones : new ArrayList<>());

                        recetas.add(receta);
                    }

                    callback.accept(recetas);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error al obtener recetas", e);
                    callback.accept(new ArrayList<>());
                });
    }

    public static void obtenerRecetaPorId(String recetaId, Consumer<Receta> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("recetas").document(recetaId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Receta receta = new Receta();
                        receta.setId(doc.getId());
                        receta.setNombre(doc.getString("nombre"));
                        receta.setDescripcion(doc.getString("descripcion"));
                        receta.setImagen(doc.getString("imagen"));
                        receta.setDificultad(doc.getString("dificultad"));
                        receta.setDuracion(doc.getString("duracion"));
                        receta.setFavorito(Boolean.TRUE.equals(doc.getBoolean("favorito")));
                        receta.setCreadorId(doc.getString("creadorId"));

                        List<DocumentReference> ingredientes = (List<DocumentReference>) doc.get("ingredientes");
                        receta.setIngredientes(ingredientes != null ? ingredientes : new ArrayList<>());

                        List<DocumentReference> instrucciones = (List<DocumentReference>) doc.get("instrucciones");
                        receta.setInstrucciones(instrucciones != null ? instrucciones : new ArrayList<>());

                        callback.accept(receta);
                    } else {
                        callback.accept(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error al obtener receta por ID", e);
                    callback.accept(null);
                });
    }
}
