package com.example.cookbooktfg;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MenuPrincipalActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecetaAdapter adapter;
    private List<Receta> recetaList;
    private EditText etBuscar;
    private ImageButton btnFiltro;
    private List<String> filtrosSeleccionados = new ArrayList<>();
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabCrearReceta;
    private ActivityResultLauncher<Intent> crearRecetaLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_principal_activity);

        recyclerView = findViewById(R.id.recyclerViewRecetas);
        etBuscar = findViewById(R.id.etBuscar);
        btnFiltro = findViewById(R.id.btnFiltro);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_inicio);

        recetaList = new ArrayList<>();
        adapter = new RecetaAdapter(recetaList, this,true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        crearRecetaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Vuelve a cargar las recetas desde Firestore
                        obtenerRecetasDeFirestore();
                    }
                }
        );

        fabCrearReceta = findViewById(R.id.fab_crear_receta);

// Abrir actividad para crear receta
        fabCrearReceta.setOnClickListener(v -> {
            Intent intent = new Intent(MenuPrincipalActivity.this, CrearRecetaActivity.class);
            crearRecetaLauncher.launch(intent);
        });


// Ocultar/mostrar FAB al hacer scroll
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && fabCrearReceta.isShown()) {
                    fabCrearReceta.hide();
                } else if (dy < 0 && !fabCrearReceta.isShown()) {
                    fabCrearReceta.show();
                }
            }
        });



        obtenerRecetasDeFirestore();
        configurarBuscador();
        configurarBottomNavigation();
    }

    private void configurarBuscador() {
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filtrarPorNombre(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnFiltro.setOnClickListener(v -> {
            obtenerIngredientesDisponibles(ingredientes -> {
                Collections.sort(ingredientes, (i1, i2) -> {
                    int tipoCompare = i1.getTipo().compareTo(i2.getTipo());
                    if (tipoCompare != 0) return tipoCompare;
                    return i1.getNombre().compareTo(i2.getNombre());
                });

                FiltroIngredientes.mostrar(this, ingredientes, new ArrayList<>(filtrosSeleccionados), nuevosSeleccionados -> {
                    filtrosSeleccionados.clear();
                    filtrosSeleccionados.addAll(nuevosSeleccionados);
                    adapter.filtrarPorIngredientesSeleccionados(filtrosSeleccionados);
                });

            });
        });


    }

    private void configurarBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                return true;
            } else if (id == R.id.nav_favoritos) {
                Intent intent = new Intent(this, FavoritosActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);

                return true;
            } else if (id == R.id.nav_historial) {
                Intent intent = new Intent(this, HistorialActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_ajustes) {
                Intent intent = new Intent(this, AjustesUserActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);

                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        obtenerRecetasDeFirestore(); // Esto recarga el adaptador y actualiza favoritos
    }

    private void obtenerRecetasDeFirestore() {
        RecetaRepositorio.obtenerTodasLasRecetas(recetas -> {
            adapter.actualizarRecetas(recetas);
            Log.d("Firestore", "Recetas cargadas: " + recetas.size());
        });
    }

    private void obtenerIngredientesDisponibles(OnIngredientesCargadosListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("ingredientes")
                .orderBy("tipo")  // Ordenar por tipo para mejor agrupación
                .orderBy("nombre")  // Luego por nombre
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<IngredienteModelo> ingredientes = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String tipo = doc.getString("tipo");
                        String nombre = doc.getString("nombre");

                        if (tipo != null && nombre != null) {
                            IngredienteModelo ingrediente = new IngredienteModelo(tipo, nombre, "");
                            ingrediente.setId(doc.getId());
                            ingredientes.add(ingrediente);
                        }
                    }
                    listener.onIngredientesCargados(ingredientes);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar ingredientes", Toast.LENGTH_SHORT).show();
                    Log.e("MenuPrincipal", "Error al cargar ingredientes", e);
                });
    }



    public interface OnIngredientesCargadosListener {
        void onIngredientesCargados(List<IngredienteModelo> ingredientes);
    }




}