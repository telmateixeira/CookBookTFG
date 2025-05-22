package com.example.cookbooktfg;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class FavoritosActivity extends AppCompatActivity {

    private RecyclerView recyclerViewRecetasFav;
    private RecetaAdapter adapter;
    private List<Receta> recetaFavs = new ArrayList<>();
    private BottomNavigationView bottomNavigationViewFav;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.favoritos_activity);

        // Inicialización de vistas
        recyclerViewRecetasFav = findViewById(R.id.recyclerViewRecetasFav);
        bottomNavigationViewFav = findViewById(R.id.bottomNavigationViewFav);
        tvEmpty = findViewById(R.id.tvEmpty);

        // Configuración del RecyclerView
        recyclerViewRecetasFav.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecetaAdapter(recetaFavs, this, true);
        recyclerViewRecetasFav.setAdapter(adapter);

        adapter.setOnFavoritoCambiadoListener((receta, posicion) -> {
            adapter.eliminarRecetaEnPosicion(posicion);
            Log.d("Favoritos", "Receta eliminada visualmente: " + receta.getNombre());

            if (adapter.getItemCount() == 0) {
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });

        configurarBottomNavigation();
        // Carga los datos reales
        cargarRecetasFavoritas();
    }

    private void configurarBottomNavigation(){
        bottomNavigationViewFav.setSelectedItemId(R.id.nav_favoritos);
        bottomNavigationViewFav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, MenuPrincipalActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return true;
            } else if (id == R.id.nav_historial) {
                startActivity(new Intent(this, HistorialActivity.class)
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

    private void cargarRecetasFavoritas() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        RecetaRepositorio.obtenerRecetasFavoritas(recetas -> {
            Log.d("Favoritos", "Recetas favoritas recibidas: " + recetas.size());

            // Update the local list
            recetaFavs.clear();
            recetaFavs.addAll(recetas);

            // Update the adapter
            adapter.actualizarRecetas(recetaFavs);

            // Update empty state
            tvEmpty.setVisibility(recetaFavs.isEmpty() ? View.VISIBLE : View.GONE);

            for (Receta r : recetaFavs) {
                Log.d("Favoritos", "→ " + r.getNombre());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarRecetasFavoritas();
    }
}
