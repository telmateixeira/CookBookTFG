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

/**
 * Actividad que muestra las recetas marcadas como favoritas por el usuario.
 * Permite visualizar y eliminar recetas favoritas.
 * <p>
 * Autor: Telma Teixeira
 * Proyecto: CookbookTFG
 */
public class FavoritosActivity extends AppCompatActivity {

    private RecyclerView recyclerViewRecetasFav;
    private RecetaAdapter adapter;
    private List<Receta> recetaFavs = new ArrayList<>();
    private BottomNavigationView bottomNavigationViewFav;
    private TextView tvEmpty;

    /**
     * Metodo llamado al crear la actividad.
     * Configura la interfaz, inicializa el RecyclerView y carga las recetas favoritas.
     */
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

    /**
     * Configura la navegación inferior para cambiar entre actividades.
     */
    private void configurarBottomNavigation() {
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

    /**
     * Carga desde el repositorio las recetas favoritas del usuario actual
     * y actualiza la interfaz.
     */
    private void cargarRecetasFavoritas() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        RecetaRepositorio.obtenerRecetasFavoritas(recetas -> {
            Log.d("Favoritos", "Recetas favoritas recibidas: " + recetas.size());
            // Actualiza la lista local
            recetaFavs.clear();
            recetaFavs.addAll(recetas);
            // Actualiza el adaptador con las nuevas recetas
            adapter.actualizarRecetas(recetaFavs);
            // Muestra u oculta el mensaje de "sin favoritos"
            tvEmpty.setVisibility(recetaFavs.isEmpty() ? View.VISIBLE : View.GONE);
            // Imprimir recetas en log (debug)
            for (Receta r : recetaFavs) {
                Log.d("Favoritos", "→ " + r.getNombre());
            }
        });
    }
    /**
     * Se llama cada vez que la actividad se vuelve visible.
     * Recarga las recetas favoritas para mantener la lista actualizada.
     */
    @Override
    protected void onResume() {
        super.onResume();
        cargarRecetasFavoritas();
    }
}
