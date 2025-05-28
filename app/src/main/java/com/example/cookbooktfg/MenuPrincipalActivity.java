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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/**
 * Actividad principal del menú de la aplicación.
 * Muestra un listado de recetas, permite buscarlas por nombre o filtrarlas por ingredientes,
 * y permite al usuario navegar a otras secciones como favoritos, historial o ajustes.
 *
 *  Autor: Telma Teixeira
 *  Proyecto: CookbookTFG
 */
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

    /**
     * Se ejecuta al crear la actividad. Inicializa las vistas, listeners,
     * y obtiene las recetas desde Firestore.
     */
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

        // Listener para recargar recetas al volver de CrearRecetaActivity
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

        // Genera recetas aleatorias si aún no existen
        String usuarioActualId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        RecetasGenerator generator = new RecetasGenerator(usuarioActualId);
        generator.generarRecetasSiEsNecesario(5, new RecetasGenerator.RecetasCallback() {
            @Override
            public void onSuccess(int recetasGeneradas) {
                Log.d("MainActivity", "Recetas generadas: " + recetasGeneradas);
                // Aquí puedes actualizar UI o cargar datos
            }

            @Override
            public void onRecetasExisten() {
                Log.d("MainActivity", "Las recetas ya existen en la base de datos");
                // Cargar recetas desde Firestore sin generar
            }

            @Override
            public void onError(String mensaje) {
                Log.e("MainActivity", "Error al generar recetas: " + mensaje);
                // Mostrar error en UI o retry
            }
        });
    }
    /**
     * Configura el buscador para filtrar recetas por nombre y botón para aplicar filtros por ingredientes.
     */
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
    /**
     * Configura el menú de navegación inferior y gestiona el cambio de actividad.
     */
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
    /**
     * Configura el menú de navegación inferior y gestiona el cambio de actividad.
     */
    @Override
    protected void onResume() {
        super.onResume();
        obtenerRecetasDeFirestore(); // Esto recarga el adaptador y actualiza favoritos
    }
    /**
     * Obtiene todas las recetas desde Firestore usando el repositorio.
     */
    private void obtenerRecetasDeFirestore() {
        RecetaRepositorio.obtenerTodasLasRecetas(recetas -> {
            adapter.actualizarRecetas(recetas);
            Log.d("Firestore", "Recetas cargadas: " + recetas.size());
        });
    }

    /**
     * Obtiene todos los ingredientes disponibles desde Firestore y los pasa a través del listener.
     * @param listener Listener que recibe la lista de ingredientes cargados.
     */
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
    /**
     * Interfaz para recibir los ingredientes disponibles desde Firestore.
     */
    public interface OnIngredientesCargadosListener {
        void onIngredientesCargados(List<IngredienteModelo> ingredientes);
    }
}