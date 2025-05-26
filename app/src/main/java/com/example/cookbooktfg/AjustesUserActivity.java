package com.example.cookbooktfg;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class AjustesUserActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private MisRecetasAdapter adapter;
    private List<Receta> listaRecetasCreadas;
    private TextView nombreUser, emailUser;
    private CircleImageView imagenPerfil;
    private BottomNavigationView bottomNavigationViewUser;
    private Button btnEditarPerfil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ajustes_usuario_activity);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        bottomNavigationViewUser = findViewById(R.id.bottomNavigationViewUser);
        nombreUser = findViewById(R.id.textViewNombreUser);
        emailUser = findViewById(R.id.textViewEmail);
        imagenPerfil = findViewById(R.id.imgPerfil);
        btnEditarPerfil = findViewById(R.id.botonEditarPerfil);
        recyclerView = findViewById(R.id.recyclerViewMisRecetas);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        listaRecetasCreadas = new ArrayList<>();

        // Configurar el adaptador con el nuevo MisRecetasAdapter
        configurarAdaptador();

        configurarBottomNavigation();
        cargarDatosUsuario();
        cargarRecetasUsuario();

        btnEditarPerfil.setOnClickListener(v -> {
            startActivity(new Intent(this, EditarRecetaActivity.class));
            finish();
        });
    }

    private void configurarAdaptador() {
        adapter = new MisRecetasAdapter(listaRecetasCreadas, new MisRecetasAdapter.OnRecipeActionListener() {
            @Override
            public void onEditRecipe(Receta receta) {
                editarReceta(receta);
            }

            @Override
            public void onDeleteRecipe(Receta receta) {
                mostrarDialogoConfirmacionEliminar(receta);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void cargarDatosUsuario() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            db.collection("usuarios").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String nombre = documentSnapshot.getString("nombre");
                            String email = user.getEmail();
                            String fotoUrl = documentSnapshot.getString("fotoPerfil");

                            nombreUser.setText(nombre != null ? nombre : "Sin nombre");
                            emailUser.setText(email);

                            if (fotoUrl != null && !fotoUrl.isEmpty()) {
                                Glide.with(this).load(fotoUrl).into(imagenPerfil);
                            } else {
                                imagenPerfil.setImageResource(R.drawable.usuario_icono);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al cargar datos del usuario", Toast.LENGTH_SHORT).show();
                    });
        }
    }


    private void cargarRecetasUsuario() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("recetas")
                .whereEqualTo("creadorId", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaRecetasCreadas.clear();
                    Log.d("Ajustes", "Recetas encontradas: " + queryDocumentSnapshots.size());
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Receta receta = doc.toObject(Receta.class);
                        receta.setId(doc.getId());
                        listaRecetasCreadas.add(receta);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar recetas", Toast.LENGTH_SHORT).show();
                });
    }

    private void editarReceta(Receta receta) {
        Intent intent = new Intent(this, EditarRecetaActivity.class);
        intent.putExtra("recetaId", receta.getId());
        startActivity(intent);
    }

    private void mostrarDialogoConfirmacionEliminar(Receta receta) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar receta")
                .setMessage("¿Estás seguro de que quieres eliminar esta receta?")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarReceta(receta))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarReceta(Receta receta) {
        db.collection("recetas").document(receta.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    int position = listaRecetasCreadas.indexOf(receta);
                    if (position != -1) {
                        listaRecetasCreadas.remove(position);
                        adapter.notifyItemRemoved(position);
                    }
                    Toast.makeText(this, "Receta eliminada", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al eliminar receta", Toast.LENGTH_SHORT).show();
                });
    }

    private void configurarBottomNavigation() {
        bottomNavigationViewUser.setSelectedItemId(R.id.nav_ajustes);
        bottomNavigationViewUser.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, MenuPrincipalActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_favoritos) {
                startActivity(new Intent(this, FavoritosActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_historial) {
                startActivity(new Intent(this, HistorialActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_ajustes) {
                // No hacer nada porque ya estamos en Ajustes
                return true;
            }
            return false;
        });
    }
}