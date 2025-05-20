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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class HistorialActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecetaAdapter adapter;
    private List<Receta> listaRecetas = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.historial_activity);

        recyclerView = findViewById(R.id.recyclerViewRecetasHist);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        bottomNavigationView = findViewById(R.id.bottomNavigationViewHist);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecetaAdapter(listaRecetas, this);
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
        bottomNavigationView.setSelectedItemId(R.id.nav_historial);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, MenuPrincipalActivity.class));
                finish();
            } else if (id == R.id.nav_favoritos) {
                startActivity(new Intent(this, FavoritosActivity.class));
                finish();
            } else if (id == R.id.nav_historial) {
                // Ya estamos aquí
                return true;
            } else if (id == R.id.nav_ajustes) {
                startActivity(new Intent(this, AjustesUserActivity.class));
                finish();
            }
            return true;
        });
    }

    private void cargarHistorial() {
        progressBar.setVisibility(View.VISIBLE);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("usuarios")
                .document(userId)
                .collection("historial")
                .orderBy("fechaVisita", Query.Direction.ASCENDING)
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
                            Log.d("Historial", "Recetas recuperadas: " + recetas.size());
                            runOnUiThread(() -> {
                                adapter.actualizarRecetas(recetas);
                                progressBar.setVisibility(View.GONE);
                                if (recetas.isEmpty()) {
                                    tvEmpty.setVisibility(View.VISIBLE);
                                } else {
                                    tvEmpty.setVisibility(View.GONE);
                                }
                            });
                        });



                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Error al cargar historial.");
                });
    }


    private void mostrarVacio() {
        progressBar.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        listaRecetas.clear();
        adapter.notifyDataSetChanged();
    }
}
