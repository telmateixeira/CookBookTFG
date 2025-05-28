package com.example.cookbooktfg;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DetalleRecetaActivity muestra la información completa de una receta seleccionada,
 * incluyendo su nombre, autor, dificultad, duración, descripción, ingredientes,
 * instrucciones e imagen. También registra la visita de la receta en el historial
 * del usuario autenticado.
 *
 *  Autor: Telma Teixeira
 *  Proyecto: CookbookTFG
 */
public class DetalleRecetaActivity extends AppCompatActivity {

    private TextView titulo, autor, dificultad, duracion, descripcion, instrucciones;
    private ImageView imagen;
    private ImageButton btnVolver;
    private IngredientesAdapter adapter;
    private RecyclerView rvingredientes;

    /**
     * Metodo llamado al crear la actividad. Inicializa la interfaz, obtiene el ID
     * de la receta desde el Intent y comienza la carga de datos.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detalle_receta_activity);

        // Inicializar vistas
        titulo = findViewById(R.id.detalleTitulo);
        autor = findViewById(R.id.detalleAutor);
        dificultad = findViewById(R.id.detalleDificultad);
        duracion = findViewById(R.id.detalleDuracion);
        descripcion = findViewById(R.id.detalleDescripcion);
        rvingredientes = findViewById(R.id.rvIngredientes);
        rvingredientes.setLayoutManager(new LinearLayoutManager(this));
        instrucciones = findViewById(R.id.detalleInstrucciones);
        imagen = findViewById(R.id.detalleImagen);
        btnVolver = findViewById(R.id.btnVolver);

        btnVolver.setOnClickListener(v -> finish());

        // Obtener ID de la receta del Intent
        String recetaId = getIntent().getStringExtra("recetaId");
        if (recetaId != null) {
            cargarReceta(recetaId);
        }
    }

    /**
     * Carga los datos de la receta desde Firestore.
     *
     * @param recetaId ID del documento de receta.
     */
    private void cargarReceta(String recetaId) {
        FirebaseFirestore.getInstance()
                .collection("recetas")
                .document(recetaId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        mostrarDatosReceta(documentSnapshot);
                        registrarVisitaEnHistorial(documentSnapshot.getId());
                    } else {
                        Toast.makeText(this, "Receta no encontrada", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar receta", Toast.LENGTH_SHORT).show();
                    Log.e("DetalleReceta", "Error: ", e);
                    finish();
                });
    }
    /**
     * Muestra los datos de la receta en la interfaz.
     *
     * @param document Snapshot del documento de la receta.
     */
    private void mostrarDatosReceta(DocumentSnapshot document) {
        // Datos básicos
        titulo.setText(document.getString("nombre"));
        descripcion.setText(document.getString("descripcion"));
        dificultad.setText(document.getString("dificultad"));
        duracion.setText(document.getString("duracion") + " min");

        // Cargar imagenes con Glide
        List<String> imagenesUrls = (List<String>) document.get("imagenes");
        if (imagenesUrls != null && !imagenesUrls.isEmpty()) {
            Glide.with(this)
                    .load(imagenesUrls.get(0))
                    .placeholder(R.drawable.placeholder)
                    .into(imagen);
        } else {
            imagen.setImageResource(R.drawable.placeholder);
        }
        // Cargar autor
        String creadorId = document.getString("creadorId");
        if (creadorId != null) {
            cargarNombreUsuario(creadorId);
        }

        // Procesar ingredientes
        List<DocumentReference> ingredientesRefs = (List<DocumentReference>) document.get("ingredientes");
        if (ingredientesRefs != null && !ingredientesRefs.isEmpty()) {
            cargarIngredientes(ingredientesRefs);
        } else {
            // Mostrar mensaje si no hay ingredientes
            TextView tvSinIngredientes = new TextView(this);
            tvSinIngredientes.setText("No hay ingredientes disponibles");
            rvingredientes.setAdapter(null);
            rvingredientes.addView(tvSinIngredientes);
        }

        // Procesar instrucciones
        List<DocumentReference> instruccionesRefs = (List<DocumentReference>) document.get("instrucciones");
        if (instruccionesRefs != null) {
            cargarInstrucciones(instruccionesRefs);
        } else {
            // Alternativa si las instrucciones están como texto directo
            String instruccionesText = document.getString("instrucciones");
            if (instruccionesText != null) {
                instrucciones.setText(instruccionesText);
            }
        }
    }
    /**
     * Carga y muestra el nombre del autor a partir del ID del usuario.
     *
     * @param userId ID del usuario creador de la receta.
     */
    private void cargarNombreUsuario(String userId) {
        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(userId)
                .get()
                .addOnSuccessListener(userDocument -> {
                    if (userDocument.exists()) {
                        autor.setText(userDocument.getString("nombre"));
                    }
                });
    }
    /**
     * Carga y muestra la lista de ingredientes desde sus referencias.
     *
     * @param ingredientesRefs Lista de referencias a documentos de ingredientes.
     */
    private void cargarIngredientes(List<DocumentReference> ingredientesRefs) {
        List<Map<String, Object>> ingredientesList = new ArrayList<>();
        AtomicInteger pendientes = new AtomicInteger(ingredientesRefs.size());

        for (DocumentReference ref : ingredientesRefs) {
            ref.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Map<String, Object> ingrediente = documentSnapshot.getData();
                    if (ingrediente != null) {
                        ingredientesList.add(ingrediente);
                    }
                }

                // Cuando todos los ingredientes estén cargados
                if (pendientes.decrementAndGet() == 0) {
                    if (!ingredientesList.isEmpty()) {
                        adapter = new IngredientesAdapter(ingredientesList);
                        rvingredientes.setAdapter(adapter);
                    } else {
                        // Mostrar mensaje si no se cargaron ingredientes
                        TextView tvSinIngredientes = new TextView(this);
                        tvSinIngredientes.setText("No se pudieron cargar los ingredientes");
                        rvingredientes.setAdapter(null);
                        rvingredientes.addView(tvSinIngredientes);
                    }
                }
            }).addOnFailureListener(e -> {
                Log.e("DetalleReceta", "Error al cargar ingrediente: ", e);
                if (pendientes.decrementAndGet() == 0) {
                    if (!ingredientesList.isEmpty()) {
                        adapter = new IngredientesAdapter(ingredientesList);
                        rvingredientes.setAdapter(adapter);
                    } else {
                        TextView tvError = new TextView(this);
                        tvError.setText("Error al cargar ingredientes");
                        rvingredientes.setAdapter(null);
                        rvingredientes.addView(tvError);
                    }
                }
            });
        }
    }
    /**
     * Carga, ordena y muestra las instrucciones de la receta.
     *
     * @param instruccionesRefs Lista de referencias a instrucciones.
     */
    private void cargarInstrucciones(List<DocumentReference> instruccionesRefs) {
        List<InstruccionModelo> instruccionesList = new ArrayList<>();
        AtomicInteger contador = new AtomicInteger(0);

        for (DocumentReference ref : instruccionesRefs) {
            ref.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    int orden = documentSnapshot.getLong("orden").intValue();
                    String paso = documentSnapshot.getString("paso");
                    instruccionesList.add(new InstruccionModelo(orden, paso));
                }

                if (contador.incrementAndGet() == instruccionesRefs.size()) {
                    // Ordenar la lista por orden ascendente
                    instruccionesList.sort((i1, i2) -> Integer.compare(i1.getOrden(), i2.getOrden()));

                    StringBuilder instruccionesText = new StringBuilder();
                    for (InstruccionModelo instruccion : instruccionesList) {
                        instruccionesText.append(instruccion.getOrden())
                                .append(". ")
                                .append(instruccion.getPaso())
                                .append("\n\n");
                    }
                    instrucciones.setText(instruccionesText.toString());
                }
            });
        }
    }

    /**
     * Registra la visita del usuario a la receta en su historial personal
     *
     * @param recetaId ID del documento de la receta visitada.
     */
    private void registrarVisitaEnHistorial(String recetaId) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId == null || recetaId == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference recetaRef = db.collection("recetas").document(recetaId);
        CollectionReference historialRef = db.collection("usuarios").document(userId).collection("historial");

        historialRef.whereEqualTo("recetaId", recetaRef)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Ya existe: actualizar fechaVisita
                        DocumentReference docExistente = queryDocumentSnapshots.getDocuments().get(0).getReference();
                        docExistente.update("fechaVisita", com.google.firebase.firestore.FieldValue.serverTimestamp());
                        Log.d("Historial", "Historial actualizado");
                    } else {
                        // No existe: crear nueva entrada
                        Map<String, Object> entradaHistorial = new HashMap<>();
                        entradaHistorial.put("recetaId", recetaRef);
                        entradaHistorial.put("fechaVisita", com.google.firebase.firestore.FieldValue.serverTimestamp());

                        historialRef.add(entradaHistorial)
                                .addOnSuccessListener(documentReference ->
                                        Log.d("Historial", "Nueva entrada en historial creada"))
                                .addOnFailureListener(e ->
                                        Log.e("Historial", "Error al crear entrada en historial", e));
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("Historial", "Error al comprobar historial", e));
    }

}