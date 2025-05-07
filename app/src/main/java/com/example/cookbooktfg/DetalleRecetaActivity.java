package com.example.cookbooktfg;


import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class DetalleRecetaActivity extends AppCompatActivity {

    private ImageView imagenReceta;
    private TextView tituloReceta, autorReceta, descripcionReceta, dificultadReceta, duracionReceta;
    private TextView detalleIngredientes, detalleInstrucciones;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detalle_receta_activity);

        // Enlazar vistas
        imagenReceta = findViewById(R.id.detalleImagen);
        tituloReceta = findViewById(R.id.detalleTitulo);
        autorReceta = findViewById(R.id.detalleAutor);
        descripcionReceta = findViewById(R.id.detalleDescripcion);
        dificultadReceta = findViewById(R.id.detalleDificultad);
        duracionReceta = findViewById(R.id.detalleDuracion);
        detalleIngredientes = findViewById(R.id.detalleIngredientes);
        detalleInstrucciones = findViewById(R.id.detalleInstrucciones);

        db = FirebaseFirestore.getInstance();

        // Obtener ID de receta
        String recetaId = getIntent().getStringExtra("recetaId");

        if (recetaId != null && !recetaId.isEmpty()) {
            cargarRecetaDesdeFirebase(recetaId);
        } else {
            Toast.makeText(this, "ID de receta no válido", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void cargarRecetaDesdeFirebase(String recetaId) {
        db.collection("recetas").document(recetaId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String titulo = documentSnapshot.getString("titulo");
                        String autor = documentSnapshot.getString("autor");
                        String descripcion = documentSnapshot.getString("descripcion");
                        String dificultad = documentSnapshot.getString("dificultad");
                        String duracion = documentSnapshot.getString("duracion");
                        String urlImagen = documentSnapshot.getString("urlImagen");
                        List<String> ingredientes = (List<String>) documentSnapshot.get("ingredientes");
                        String instrucciones = documentSnapshot.getString("instrucciones");

                        tituloReceta.setText(titulo);
                        autorReceta.setText(autor);
                        descripcionReceta.setText(descripcion);
                        dificultadReceta.setText(dificultad);
                        duracionReceta.setText(duracion);

                        // Imagen con Glide
                        Glide.with(this)
                                .load(urlImagen)
                                .placeholder(R.drawable.placeholder)
                                .into(imagenReceta);

                        // Ingredientes
                        if (ingredientes != null) {
                            StringBuilder textoIngredientes = new StringBuilder();
                            for (String ing : ingredientes) {
                                textoIngredientes.append("• ").append(ing).append("\n");
                            }
                            detalleIngredientes.setText(textoIngredientes.toString().trim());
                        }

                        // Instrucciones
                        if (instrucciones != null) {
                            detalleInstrucciones.setText(instrucciones);
                        }
                    } else {
                        Toast.makeText(this, "Receta no encontrada", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar la receta", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}
