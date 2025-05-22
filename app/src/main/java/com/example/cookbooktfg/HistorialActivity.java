package com.example.cookbooktfg;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookbooktfg.RecetaAdapter;
import com.example.cookbooktfg.Receta;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class HistorialActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecetaAdapter adapter;
    private List<Receta> listaRecetas = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private BottomNavigationView bottomNavigationViewHist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.historial_activity);

        recyclerView = findViewById(R.id.recyclerViewRecetasHist);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        bottomNavigationViewHist = findViewById(R.id.bottomNavigationViewHist);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecetaAdapter(listaRecetas, this, true);
        adapter.setOnRecetaClickListener(recetaId -> {
            Intent intent = new Intent(HistorialActivity.this, DetalleRecetaActivity.class);
            intent.putExtra("recetaId", recetaId);
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);


        configurarBottomNavigation();
        cargarHistorial();
    }

    private void configurarBottomNavigation() {
        bottomNavigationViewHist.setSelectedItemId(R.id.nav_historial);
        bottomNavigationViewHist.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, MenuPrincipalActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return true;
            } else if (id == R.id.nav_favoritos) {
                startActivity(new Intent(this, FavoritosActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return true;
            } else if (id == R.id.nav_ajustes) {
                startActivity(new Intent(this, AjustesUserActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return true;
            }
            return false;
        });
    }

//    private void cargarHistorial() {
//        progressBar.setVisibility(View.VISIBLE);
//        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//
//        // Primero obtenemos los IDs de las recetas favoritas
//        db.collection("usuarios")
//                .document(userId)
//                .collection("favoritos")
//                .get()
//                .addOnSuccessListener(favoritosDocs -> {
//                    List<String> favoritoIds = new ArrayList<>();
//                    for (DocumentSnapshot doc : favoritosDocs) {
//                        favoritoIds.add(doc.getId());
//                    }
//
//                    // Luego obtenemos el historial
//                    db.collection("usuarios")
//                            .document(userId)
//                            .collection("historial")
//                            .orderBy("fechaVisita", Query.Direction.DESCENDING)
//                            .limit(50)
//                            .get()
//                            .addOnSuccessListener(historialDocs -> {
//                                List<String> recetaIds = new ArrayList<>();
//                                for (DocumentSnapshot doc : historialDocs) {
//                                    DocumentReference recetaRef = doc.getDocumentReference("recetaId");
//                                    if (recetaRef != null) {
//                                        recetaIds.add(recetaRef.getId());
//                                    }
//                                }
//
//                                if (recetaIds.isEmpty()) {
//                                    mostrarVacio();
//                                } else {
//                                    RecetaRepositorio.obtenerRecetasPorIds(recetaIds, recetas -> {
//                                        // Marcamos las recetas favoritas antes de pasarlas al adaptador
//                                        for (Receta receta : recetas) {
//                                            receta.setFavorito(favoritoIds.contains(receta.getId()));
//                                            Log.d("Historial",favoritoIds.contains(receta.getId()) + " " );
//                                        }
//
//                                        runOnUiThread(() -> {
//                                            adapter.actualizarRecetas(recetas);
//                                            progressBar.setVisibility(View.GONE);
//                                            tvEmpty.setVisibility(recetas.isEmpty() ? View.VISIBLE : View.GONE);
//                                        });
//                                    });
//                                }
//                            })
//                            .addOnFailureListener(e -> {
//                                progressBar.setVisibility(View.GONE);
//                                tvEmpty.setVisibility(View.VISIBLE);
//                                tvEmpty.setText("Error al cargar historial.");
//                            });
//                })
//                .addOnFailureListener(e -> {
//                    progressBar.setVisibility(View.GONE);
//                    tvEmpty.setVisibility(View.VISIBLE);
//                    tvEmpty.setText("Error al cargar favoritos.");
//                });
//
//
//
//    }

    private void cargarHistorial() {
        progressBar.setVisibility(View.VISIBLE);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Primero obtenemos las referencias de las recetas favoritas (igual que en FavoritosActivity)
        db.collection("usuarios")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<DocumentReference> favoritasRefs = (List<DocumentReference>) documentSnapshot.get("favoritos");
                    Set<String> favoritoIds = new HashSet<>();

                    if (favoritasRefs != null) {
                        for (DocumentReference ref : favoritasRefs) {
                            favoritoIds.add(ref.getId());
                        }
                    }

                    // Luego obtenemos el historial
                    db.collection("usuarios")
                            .document(userId)
                            .collection("historial")
                            .orderBy("fechaVisita", Query.Direction.DESCENDING)
                            .limit(50)
                            .get()
                            .addOnSuccessListener(historialDocs -> {
                                List<String> recetaIds = new ArrayList<>();
                                for (DocumentSnapshot doc : historialDocs) {
                                    DocumentReference recetaRef = doc.getDocumentReference("recetaId");
                                    if (recetaRef != null) {
                                        recetaIds.add(recetaRef.getId());
                                    }
                                }

                                if (recetaIds.isEmpty()) {
                                    mostrarVacio();
                                } else {
                                    RecetaRepositorio.obtenerRecetasPorIds(recetaIds, recetas -> {
                                        // Marcamos las recetas favoritas
                                        for (Receta receta : recetas) {
                                            boolean esFavorita = favoritoIds.contains(receta.getId());
                                            receta.setFavorito(esFavorita);
                                            Log.d("Historial", "Receta " + receta.getNombre() +
                                                    " - Favorita: " + esFavorita);
                                        }

                                        runOnUiThread(() -> {
                                            adapter.actualizarRecetas(recetas);
                                            progressBar.setVisibility(View.GONE);
                                            tvEmpty.setVisibility(recetas.isEmpty() ? View.VISIBLE : View.GONE);
                                        });
                                    });
                                }
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                tvEmpty.setVisibility(View.VISIBLE);
                                tvEmpty.setText("Error al cargar historial.");
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Error al cargar favoritos.");
                });
    }


    private void mostrarVacio() {
        progressBar.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        listaRecetas.clear();
        adapter.notifyDataSetChanged();
    }
}
