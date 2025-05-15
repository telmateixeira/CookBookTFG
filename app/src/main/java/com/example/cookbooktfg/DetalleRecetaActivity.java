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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DetalleRecetaActivity extends AppCompatActivity {

    private TextView titulo, autor, dificultad, duracion, descripcion, instrucciones;
    private ImageView imagen;
    private ImageButton btnVolver;
    private IngredientesAdapter adapter;
    private RecyclerView rvingredientes;

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

    private void cargarReceta(String recetaId) {
        FirebaseFirestore.getInstance()
                .collection("recetas")
                .document(recetaId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        mostrarDatosReceta(documentSnapshot);
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

    private void mostrarDatosReceta(DocumentSnapshot document) {
        // Datos básicos
        titulo.setText(document.getString("nombre"));
        descripcion.setText(document.getString("descripcion"));
        dificultad.setText(document.getString("dificultad"));
        duracion.setText(document.getString("duracion") + " min");

        // Cargar imagen con Glide
        String imagenUrl = document.getString("imagen");
        if (imagenUrl != null && !imagenUrl.isEmpty()) {
            Glide.with(this)
                    .load(imagenUrl)
                    .placeholder(R.drawable.placeholder)
                    .into(imagen);
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

        // Procesar instrucciones (si están como referencias)
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
                    // Manejar caso cuando hay errores pero todos los intentos terminaron
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

    private void cargarInstrucciones(List<DocumentReference> instruccionesRefs) {
        StringBuilder instruccionesText = new StringBuilder();
        AtomicInteger contador = new AtomicInteger(0);

        for (DocumentReference ref : instruccionesRefs) {
            ref.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    int orden = documentSnapshot.getLong("orden").intValue();
                    String paso = documentSnapshot.getString("paso");

                    instruccionesText.append(orden).append(". ").append(paso).append("\n\n");
                }

                // Verificar si todas las instrucciones se cargaron
                if (contador.incrementAndGet() == instruccionesRefs.size()) {
                    instrucciones.setText(instruccionesText.toString());
                }
            });
        }
    }
}