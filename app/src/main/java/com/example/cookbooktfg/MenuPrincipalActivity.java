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

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MenuPrincipalActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecetaAdapter adapter;
    private List<Receta> recetaList;
    private EditText etBuscar;
    private ImageButton btnFiltro;
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

        recetaList = new ArrayList<>();
        adapter = new RecetaAdapter(recetaList, this);
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
                adapter.filtrarPorIngrediente(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnFiltro.setOnClickListener(v -> {
            Toast.makeText(this, "Abrir filtro de ingredientes", Toast.LENGTH_SHORT).show();
        });
    }

    private void configurarBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                return true;
            } else if (id == R.id.nav_favoritos) {
                startActivity(new Intent(this, FavoritosActivity.class));
                return true;
            } else if (id == R.id.nav_historial) {
                startActivity(new Intent(this, HistorialActivity.class));
                return true;
            } else if (id == R.id.nav_ajustes) {
                startActivity(new Intent(this, AjustesUserActivity.class));
                return true;
            }
            return false;
        });
    }

    private void obtenerRecetasDeFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("recetas")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Receta> recetasCargadas = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Receta receta = new Receta();

                        receta.setId(doc.getId()); // ID automático de Firestore
                        receta.setNombre(doc.getString("nombre"));
                        receta.setDescripcion(doc.getString("descripcion"));
                        receta.setImagen(doc.getString("imagen"));
                        receta.setDificultad(doc.getString("dificultad"));
                        receta.setDuracion(doc.getString("duracion"));

                        // Favorito como booleano
                        Boolean favorito = doc.getBoolean("favorito");
                        receta.setFavorito(favorito != null ? favorito : false);

                        receta.setCreadorId(doc.getString("creadorId"));

                        List<DocumentReference> ingredientes = (List<DocumentReference>) doc.get("ingredientes");
                        receta.setIngredientes(ingredientes != null ? ingredientes : new ArrayList<>());

                        List<DocumentReference> instrucciones = (List<DocumentReference>) doc.get("instrucciones");
                        receta.setInstrucciones(instrucciones != null ? instrucciones : new ArrayList<>());

                        recetasCargadas.add(receta);
                        Log.d("Firestore", "Receta cargada: " + receta.getNombre());
                    }

                    //Aquí es donde deberías actualizar el adaptador
                    adapter.actualizarRecetas(recetasCargadas);
                    Log.d("Firestore", "Recetas cargadas: " + recetasCargadas.size());

                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error al obtener recetas", e));
    }


}
