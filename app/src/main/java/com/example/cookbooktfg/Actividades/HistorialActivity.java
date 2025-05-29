package com.example.cookbooktfg.Actividades;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookbooktfg.R;
import com.example.cookbooktfg.Modelos.Receta;
import com.example.cookbooktfg.Modelos.RecetaAdapter;
import com.example.cookbooktfg.Repositorio.RecetaRepositorio;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Actividad que muestra el historial de recetas vistas recientemente por el usuario.
 * Carga hasta 50 recetas del historial ordenadas por fecha de visita descendente y
 * permite al usuario acceder al detalle de cada receta.
 *
 *  Autor: Telma Teixeira
 *  Proyecto: CookbookTFG
 */
public class HistorialActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecetaAdapter adapter;
    private List<Receta> listaRecetas = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private BottomNavigationView bottomNavigationViewHist;

    /**
     * Inicializa la actividad, incluyendo el RecyclerView, adaptador y la barra de navegación.
     * También se inicia la carga del historial de recetas desde Firestore.
     */
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

    /**
     * Configura el menú de navegación inferior para permitir cambiar entre secciones
     * como inicio, favoritos y ajustes.
     */
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
    /**
     * Carga las recetas visitadas recientemente por el usuario desde Firestore.
     * También marca aquellas recetas que están en la lista de favoritos.
     * Si no hay recetas o hay un error, se muestra un mensaje correspondiente.
     */
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
                                        // Se marcan las recetas favoritas
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
    /**
     * Muestra un mensaje indicando que el historial está vacío
     * y oculta la barra de progreso.
     */
    private void mostrarVacio() {
        progressBar.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        listaRecetas.clear();
        adapter.notifyDataSetChanged();
    }
}
